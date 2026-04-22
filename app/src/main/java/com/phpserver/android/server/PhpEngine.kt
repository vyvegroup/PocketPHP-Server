package com.phpserver.android.server

import android.content.Context
import com.phpserver.android.PhpServerApp
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PhpEngine {

    private var phpBinaryPath: String? = null
    private var isInitialized = false

    init {
        initializePhp()
    }

    private fun initializePhp() {
        val context = PhpServerApp.instance
        val phpDir = File(context.filesDir, "php")

        if (!phpDir.exists()) phpDir.mkdirs()

        val phpBinary = File(phpDir, "php-cgi")
        if (phpBinary.exists() && phpBinary.canExecute()) {
            phpBinaryPath = phpBinary.absolutePath
            isInitialized = true
            detectPhpVersion()
            return
        }

        // Extract bundled PHP if available
        extractBundledPhp(context, phpDir, phpBinary)

        if (phpBinary.exists() && phpBinary.canExecute()) {
            phpBinaryPath = phpBinary.absolutePath
            isInitialized = true
            detectPhpVersion()
        }
    }

    private fun extractBundledPhp(context: Context, phpDir: File, phpBinary: File) {
        try {
            val phpAssetNames = listOf("php/php-cgi", "php-cgi", "bin/php-cgi")
            var found = false
            for (assetName in phpAssetNames) {
                if (found) break
                try {
                    context.assets.open(assetName).use { input ->
                        phpBinary.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        phpBinary.setExecutable(true)
                        ServerState.logs.add("[PhpEngine] Extracted PHP binary from $assetName")
                        found = true
                    }
                } catch (_: Exception) {
                    // Try next asset name
                }
            }
        } catch (e: Exception) {
            ServerState.logs.add("[PhpEngine] Failed to extract PHP: ${e.message}")
        }
    }

    private fun detectPhpVersion() {
        try {
            phpBinaryPath?.let { path ->
                val process = ProcessBuilder(path, "-v")
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                val versionMatch = Regex("PHP (\\d+\\.\\d+\\.\\d+)").find(output)
                versionMatch?.let {
                    ServerState.phpVersion = it.groupValues[1]
                }
                process.destroy()
            }
        } catch (e: Exception) {
            ServerState.logs.add("[PhpEngine] Could not detect PHP version: ${e.message}")
        }
    }

    fun execute(phpFile: File, session: NanoHTTPD.IHTTPSession): String {
        return if (isInitialized && phpBinaryPath != null) {
            executeWithPhpBinary(phpFile, session)
        } else {
            executeWithBuiltInInterpreter(phpFile, session)
        }
    }

    private fun executeWithPhpBinary(phpFile: File, session: NanoHTTPD.IHTTPSession): String {
        return try {
            val env = mutableListOf(
                "SCRIPT_FILENAME=${phpFile.absolutePath}",
                "REQUEST_METHOD=${session.method.name}",
                "SERVER_SOFTWARE=PHP-LocalServer/1.0",
                "SERVER_PROTOCOL=HTTP/1.1",
                "GATEWAY_INTERFACE=CGI/1.1",
                "REDIRECT_STATUS=200",
                "CONTENT_TYPE=${session.headers["content-type"] ?: ""}",
                "QUERY_STRING=${session.queryParameterString ?: ""}",
                "REQUEST_URI=${session.uri}",
                "SERVER_PORT=${ServerState.port}",
                "SERVER_NAME=localhost",
                "DOCUMENT_ROOT=${phpFile.parent}",
            )

            // Add POST data
            if (session.method == NanoHTTPD.Method.POST) {
                val contentLength = session.headers["content-length"] ?: "0"
                env.add("CONTENT_LENGTH=$contentLength")
            }

            // Add cookie
            session.headers["cookie"]?.let { env.add("HTTP_COOKIE=$it") }

            val envMap = env.associate { 
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrElse(1) { "" })
            }
            val pb = ProcessBuilder(phpBinaryPath!!)
            pb.environment().putAll(envMap)
            pb.directory(phpFile.parentFile)
            val process = pb.redirectErrorStream(true).start()

            // Write POST body if present
            if (session.method == NanoHTTPD.Method.POST) {
                val files = mutableMapOf<String, String>()
                session.parseBody(files)
            }

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            process.destroy()

            // Strip CGI headers from output
            stripCgiHeaders(output)
        } catch (e: Exception) {
            "<h1>PHP Execution Error</h1><pre>${e.message ?: "Unknown error"}</pre>"
        }
    }

    private fun executeWithBuiltInInterpreter(phpFile: File, session: NanoHTTPD.IHTTPSession): String {
        return try {
            val content = phpFile.readText()
            SimplePhpInterpreter.interpret(content, session)
        } catch (e: Exception) {
            "<h1>PHP Error</h1><pre>${e.message}</pre>"
        }
    }

    private fun stripCgiHeaders(output: String): String {
        val headerEnd = output.indexOf("\r\n\r\n")
        return if (headerEnd >= 0) output.substring(headerEnd + 4)
        else {
            val altHeaderEnd = output.indexOf("\n\n")
            if (altHeaderEnd >= 0) output.substring(altHeaderEnd + 2)
            else output
        }
    }

    fun getPhpInfo(): String {
        return """
        {
            "php_version": "${ServerState.phpVersion}",
            "server": "PHP Local Server for Android",
            "sapi": "CGI/FastCGI",
            "os": "Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.MODEL})",
            "architecture": "${System.getProperty("os.arch")}",
            "binary_available": $isInitialized,
            "binary_path": "${phpBinaryPath ?: "Built-in interpreter"}",
            "max_execution_time": "30",
            "memory_limit": "128M",
            "upload_max_filesize": "50M",
            "post_max_size": "50M",
            "extensions": ["core", "standard", "date", "json", "fileinfo"],
            "document_root": "${PhpServerApp.instance.filesDir}/www",
            "server_port": ${ServerState.port}
        }
        """.trimIndent()
    }

    fun isBinaryAvailable(): Boolean = isInitialized
}

/**
 * Minimal built-in PHP interpreter for basic PHP files.
 * Supports echo, variables, basic string operations, and HTML passthrough.
 */
object SimplePhpInterpreter {

    fun interpret(code: String, session: NanoHTTPD.IHTTPSession): String {
        val output = StringBuilder()

        val lines = code.lines()
        val variables = mutableMapOf<String, String>()

        var inPhp = false
        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.contains("<?php") || trimmed.contains("<?")) {
                inPhp = true
                val afterTag = if (trimmed.contains("<?php")) {
                    trimmed.substringAfter("<?php").trim()
                } else {
                    trimmed.substringAfter("<?").trim()
                }
                if (afterTag.isNotEmpty()) {
                    processPhpLine(afterTag, variables, output, session)
                }
                continue
            }

            if (trimmed.contains("?>")) {
                val beforeTag = trimmed.substringBefore("?>")
                if (beforeTag.trim().isNotEmpty()) {
                    processPhpLine(beforeTag.trim(), variables, output, session)
                }
                inPhp = false
                continue
            }

            if (inPhp) {
                processPhpLine(trimmed, variables, output, session)
            } else {
                output.append(line).append("\n")
            }
        }

        return output.toString()
    }

    private fun processPhpLine(
        line: String,
        variables: MutableMap<String, String>,
        output: StringBuilder,
        session: NanoHTTPD.IHTTPSession
    ) {
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) return

        // Variable assignment
        val varAssign = Regex("""\\$(\w+)\s*=\s*(.+);""").find(line)
        if (varAssign != null) {
            val varName = varAssign.groupValues[1]
            var value = varAssign.groupValues[2].trim()
            // Handle string values
            if (value.startsWith("\"") && value.endsWith("\";")) {
                value = value.substring(1, value.length - 2)
            } else if (value.startsWith("'") && value.endsWith("';")) {
                value = value.substring(1, value.length - 2)
            }
            variables[varName] = value
            return
        }

        // Echo statement
        val echoMatch = Regex("""echo\s+(.+);?""").find(line)
        if (echoMatch != null) {
            var echoContent = echoMatch.groupValues[1].trim()
            // Replace variables
            echoContent = Regex("""\\$(\w+)""").replace(echoContent) { match ->
                variables[match.groupValues[1]] ?: ""
            }
            // Handle string concatenation
            if (echoContent.startsWith("\"") && echoContent.endsWith("\";")) {
                echoContent = echoContent.substring(1, echoContent.length - 2)
            } else if (echoContent.startsWith("\"") && echoContent.endsWith("\"")) {
                echoContent = echoContent.substring(1, echoContent.length - 1)
            } else if (echoContent.startsWith("'") && echoContent.endsWith("';")) {
                echoContent = echoContent.substring(1, echoContent.length - 2)
            } else if (echoContent.startsWith("'") && echoContent.endsWith("'")) {
                echoContent = echoContent.substring(1, echoContent.length - 1)
            }
            output.append(echoContent)
            return
        }

        // phpinfo() shorthand
        if (line.contains("phpinfo()")) {
            output.append(getPhpInfoHtml(session))
            return
        }
    }

    private fun getPhpInfoHtml(session: NanoHTTPD.IHTTPSession): String {
        return """
        <!DOCTYPE html>
        <html>
        <head><title>PHP Info</title>
        <style>
            body { font-family: monospace; background: #fff; color: #333; padding: 20px; }
            h1 { color: #777BB3; }
            table { border-collapse: collapse; width: 100%; margin: 10px 0; }
            td, th { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
            th { background: #777BB3; color: white; }
            tr:nth-child(even) { background: #f9f9f9; }
        </style>
        </head>
        <body>
        <h1>PHP Information</h1>
        <table>
            <tr><th>Setting</th><th>Value</th></tr>
            <tr><td>PHP Version</td><td>${ServerState.phpVersion}</td></tr>
            <tr><td>Server</td><td>PHP Local Server for Android</td></tr>
            <tr><td>Android Version</td><td>${android.os.Build.VERSION.RELEASE}</td></tr>
            <tr><td>Device</td><td>${android.os.Build.MODEL}</td></tr>
            <tr><td>Architecture</td><td>${System.getProperty("os.arch")}</td></tr>
            <tr><td>Server Port</td><td>${ServerState.port}</td></tr>
            <tr><td>Document Root</td><td>${PhpServerApp.instance.filesDir}/www</td></tr>
            <tr><td>Request URI</td><td>${session.uri}</td></tr>
            <tr><td>Request Method</td><td>${session.method.name}</td></tr>
        </table>
        </body>
        </html>
        """.trimIndent()
    }
}
