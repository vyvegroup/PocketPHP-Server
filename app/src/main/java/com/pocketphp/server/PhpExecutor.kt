package com.pocketphp.server

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Executes PHP 8.3 scripts using the bundled PHP binary.
 * Handles CGI-like execution with proper environment variables.
 */
class PhpExecutor(private val context: Context) {

    companion object {
        private const val TAG = "PhpExecutor"
    }

    private val installer = PhpInstaller(context)
    private val phpDir = File(context.filesDir, PhpInstaller.PHP_DIR_NAME)
    private val phpBinDir = File(phpDir, "bin")
    private val phpLibDir = File(phpDir, "lib")

    /**
     * Check if PHP is installed and ready.
     */
    fun isPhpInstalled(): Boolean = installer.isInstalled()

    /**
     * Get the PHP installer for UI-driven installation.
     */
    fun getInstaller(): PhpInstaller = installer

    /**
     * Get the PHP binary file path.
     */
    fun getPhpBinaryPath(): String? {
        return if (isPhpInstalled()) installer.phpBinary.absolutePath else null
    }

    /**
     * Get the PHP directory.
     */
    fun getPhpDir(): File = phpDir

    /**
     * Execute a PHP script and return the output.
     *
     * @param scriptPath Absolute path to the PHP script
     * @param serverVars CGI server variables
     * @param postData POST data body (null for GET)
     * @return The PHP script output (body only, after CGI headers)
     */
    fun execute(scriptPath: String, serverVars: Map<String, String>, postData: String? = null): String {
        if (!isPhpInstalled()) {
            return "<h1>PHP 8.3 Not Installed</h1><p>Please tap \"Install PHP 8.3\" button to set up the PHP runtime.</p>"
        }

        val phpBinary = installer.phpBinary
        if (!phpBinary.exists()) {
            return "<h1>PHP Binary Not Found</h1><p>Please reinstall PHP from Settings.</p>"
        }

        return try {
            val scriptFile = File(scriptPath)
            if (!scriptFile.exists()) {
                return "<h1>404 Not Found</h1><p>Script not found: $scriptPath</p>"
            }

            val wwwRoot = File(context.filesDir, "www")

            val command = mutableListOf(
                phpBinary.absolutePath,
                "-d", "display_errors=On",
                "-d", "error_reporting=E_ALL",
                "-d", "short_open_tag=On",
                "-d", "doc_root=${wwwRoot.absolutePath}",
                "-d", "extension_dir=${phpLibDir.absolutePath}",
                "-d", "cgi.force_redirect=0",
                "-d", "cgi.fix_pathinfo=1"
            )

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(wwwRoot)
            processBuilder.redirectErrorStream(true)

            // Set environment variables
            val env = processBuilder.environment()
            // CRITICAL: Set LD_LIBRARY_PATH so PHP finds bundled .so libraries
            // Also append system lib paths
            val systemLibPath = System.getenv("LD_LIBRARY_PATH") ?: ""
            env["LD_LIBRARY_PATH"] = "${phpLibDir.absolutePath}:${phpDir.absolutePath}:$systemLibPath"
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = File(phpDir, "tmp").apply { mkdirs() }.absolutePath

            // CGI standard variables
            env["GATEWAY_INTERFACE"] = "CGI/1.1"
            env["SERVER_PROTOCOL"] = "HTTP/1.1"
            env["SERVER_SOFTWARE"] = "PocketPHP/1.0"
            env["DOCUMENT_ROOT"] = wwwRoot.absolutePath
            env["SCRIPT_FILENAME"] = scriptPath
            env["REDIRECT_STATUS"] = "200"

            // Pass through server variables
            serverVars.forEach { (key, value) -> env[key] = value }

            // Handle POST data
            if (postData != null) {
                env["CONTENT_LENGTH"] = postData.length.toString()
                if (serverVars["CONTENT_TYPE"] != null) {
                    env["CONTENT_TYPE"] = serverVars["CONTENT_TYPE"]!!
                } else {
                    env["CONTENT_TYPE"] = "application/x-www-form-urlencoded"
                }
            }

            Log.d(TAG, "Executing: ${command.take(4).joinToString(" ")}... script=$scriptPath")

            val process = processBuilder.start()

            // Write POST data to stdin
            if (postData != null && postData.isNotEmpty()) {
                process.outputStream.write(postData.toByteArray())
                process.outputStream.flush()
                process.outputStream.close()
            }

            // Read output with timeout protection
            val output = buildString {
                process.inputStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    val maxLines = 5000
                    var lineCount = 0
                    while (line != null && lineCount < maxLines) {
                        append(line).append("\n")
                        line = reader.readLine()
                        lineCount++
                    }
                }
            }

            val exitCode = process.waitFor()

            if (exitCode != 0 && output.isEmpty()) {
                Log.e(TAG, "PHP exited with code $exitCode (no output)")
                return "<h1>PHP Error</h1><p>PHP exited with code $exitCode. Check that all required libraries are present.</p><p>Device: ${Build.SUPPORTED_ABIS.firstOrNull()} | Libs: ${phpLibDir.listFiles()?.count() ?: 0} files</p>"
            }

            if (exitCode != 0 && output.isNotEmpty()) {
                Log.w(TAG, "PHP exited with code $exitCode (with output)")
            }

            parseCgiOutput(output)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing PHP script", e)
            val diag = installer.getDiagnostics()
            "<h1>PHP Execution Error</h1><pre>${e.message ?: "Unknown error"}\n\nDevice: ${Build.SUPPORTED_ABIS.firstOrNull()}\n\n${diag}</pre>"
        }
    }

    /**
     * Parse CGI output to extract the body (after headers).
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
        return installer.getPhpVersion()
    }
}
