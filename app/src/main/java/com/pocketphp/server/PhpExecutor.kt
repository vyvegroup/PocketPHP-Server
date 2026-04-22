package com.pocketphp.server

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Executes PHP scripts using a bundled PHP CGI binary.
 * Downloads PHP binary on first use if not present.
 */
class PhpExecutor(private val context: Context) {

    companion object {
        private const val TAG = "PhpExecutor"
        private const val PHP_DIR = "php"
        private const val PHP_BINARY_NAME = "php"
        private const val PHP_INI_NAME = "php.ini"
        private const val PHP_DOWNLOAD_URL = "https://github.com/vyvegroup/PocketPHP-Server/releases/download/php-binary/php-cgi-arm64"
    }

    private val phpDir = File(context.filesDir, PHP_DIR)
    private var phpBinary: File? = null
    private var isInstalled = false

    /**
     * Check if PHP is installed and ready.
     */
    fun isPhpInstalled(): Boolean {
        if (isInstalled && phpBinary?.exists() == true) return true
        phpBinary = File(phpDir, PHP_BINARY_NAME)
        isInstalled = phpBinary?.exists() == true && phpBinary?.canExecute() == true
        return isInstalled
    }

    /**
     * Get the PHP binary file path.
     */
    fun getPhpBinaryPath(): String? {
        return if (isPhpInstalled()) phpBinary?.absolutePath else null
    }

    /**
     * Get the PHP directory.
     */
    fun getPhpDir(): File = phpDir

    /**
     * Execute a PHP script and return the output.
     *
     * @param scriptPath Absolute path to the PHP script
     * @param serverVars CGI server variables (SCRIPT_NAME, REQUEST_URI, etc.)
     * @param postData POST data body (null for GET)
     * @return The PHP script output (stdout)
     */
    fun execute(scriptPath: String, serverVars: Map<String, String>, postData: String? = null): String {
        if (!isPhpInstalled()) {
            return "<h1>PHP not installed</h1><p>Please install PHP from the app settings.</p>"
        }

        return try {
            val command = mutableListOf(phpBinary?.absolutePath ?: return "PHP binary not found")
            command.add("-d")
            command.add("display_errors=On")
            command.add("-d")
            command.add("error_reporting=E_ALL")
            command.add("-d")
            command.add("short_open_tag=On")
            command.add("-d")
            command.add("doc_root=${context.filesDir.absolutePath}/www")
            command.add(scriptPath)

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(phpDir)
            processBuilder.redirectErrorStream(true)

            // Set CGI environment variables
            val env = processBuilder.environment()
            env["GATEWAY_INTERFACE"] = "CGI/1.1"
            env["SERVER_PROTOCOL"] = "HTTP/1.1"
            env["SERVER_SOFTWARE"] = "PocketPHP/1.0"
            env["DOCUMENT_ROOT"] = File(context.filesDir, "www").absolutePath
            serverVars.forEach { (key, value) -> env[key] = value }

            if (postData != null) {
                env["CONTENT_LENGTH"] = postData.length.toString()
                env["CONTENT_TYPE"] = "application/x-www-form-urlencoded"
                processBuilder.redirectErrorStream(true)
            }

            val process = processBuilder.start()

            // Write POST data if present
            if (postData != null) {
                process.outputStream.write(postData.toByteArray())
                process.outputStream.flush()
                process.outputStream.close()
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0 && output.isEmpty()) {
                Log.e(TAG, "PHP exited with code $exitCode")
                return "<h1>PHP Error</h1><p>PHP exited with code $exitCode</p>"
            }

            // Parse CGI output: separate headers from body
            parseCgiOutput(output)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing PHP", e)
            "<h1>PHP Execution Error</h1><pre>${e.message ?: "Unknown error"}</pre>"
        }
    }

    /**
     * Parse CGI output to extract headers and body.
     */
    private fun parseCgiOutput(raw: String): String {
        val lines = raw.lines()
        val bodyLines = mutableListOf<String>()
        var inBody = false

        for (line in lines) {
            if (!inBody) {
                if (line.isBlank()) {
                    inBody = true
                }
                // CGI headers are parsed by the caller
            } else {
                bodyLines.add(line)
            }
        }

        return bodyLines.joinToString("\n")
    }

    /**
     * Parse CGI headers from PHP output.
     */
    fun parseHeaders(raw: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val statusHeader = StringBuilder()

        for (line in raw.lines()) {
            if (line.isBlank()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                if (key.equals("Status", ignoreCase = true)) {
                    statusHeader.append(value)
                } else {
                    headers[key] = value
                }
            }
        }

        if (statusHeader.isNotEmpty()) {
            headers["X-PHP-Status"] = statusHeader.toString()
        }

        return headers
    }

    /**
     * Get PHP version string.
     */
    fun getPhpVersion(): String? {
        if (!isPhpInstalled()) return null
        return try {
            val process = ProcessBuilder(phpBinary?.absolutePath ?: return null, "-v")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.lines().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
