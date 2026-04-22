package com.pocketphp.server

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Executes PHP 8.3 scripts using the bundled PHP CGI binary.
 * Sets LD_LIBRARY_PATH for proper shared library resolution.
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
     * @return The PHP script output
     */
    fun execute(scriptPath: String, serverVars: Map<String, String>, postData: String? = null): String {
        if (!isPhpInstalled()) {
            return "<h1>PHP 8.3 Not Installed</h1><p>Please tap \"Install PHP 8.3\" button to download and set up the PHP runtime.</p>"
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

            val command = mutableListOf(
                phpBinary.absolutePath,
                "-d", "display_errors=On",
                "-d", "error_reporting=E_ALL",
                "-d", "short_open_tag=On",
                "-d", "doc_root=${File(context.filesDir, "www").absolutePath}",
                "-d", "extension_dir=${phpLibDir.absolutePath}",
                "-d", "cgi.force_redirect=0",
                "-d", "cgi.fix_pathinfo=1"
            )

            // Load custom php.ini if exists
            val iniFile = File(phpDir, "php.ini")
            if (iniFile.exists()) {
                command.add("-d")
                command.add("custom_ini=${iniFile.absolutePath}")
            }

            command.add(scriptPath)

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(phpDir)
            processBuilder.redirectErrorStream(true)

            // Set environment
            val env = processBuilder.environment()
            // Critical: Set LD_LIBRARY_PATH so PHP finds bundled .so libraries
            env["LD_LIBRARY_PATH"] = phpLibDir.absolutePath
            env["HOME"] = phpDir.absolutePath

            // CGI variables
            env["GATEWAY_INTERFACE"] = "CGI/1.1"
            env["SERVER_PROTOCOL"] = "HTTP/1.1"
            env["SERVER_SOFTWARE"] = "PocketPHP/1.0"
            env["DOCUMENT_ROOT"] = File(context.filesDir, "www").absolutePath
            env["SCRIPT_FILENAME"] = scriptPath
            env["REDIRECT_STATUS"] = "200"

            serverVars.forEach { (key, value) -> env[key] = value }

            if (postData != null) {
                env["CONTENT_LENGTH"] = postData.length.toString()
                env["CONTENT_TYPE"] = "application/x-www-form-urlencoded"
                env["REQUEST_METHOD"] = "POST"
            }

            val process = processBuilder.start()

            // Write POST data
            if (postData != null) {
                process.outputStream.write(postData.toByteArray())
                process.outputStream.flush()
                process.outputStream.close()
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0 && output.isEmpty()) {
                Log.e(TAG, "PHP exited with code $exitCode")
                return "<h1>PHP Error</h1><p>PHP exited with code $exitCode. Check that all required libraries are present.</p>"
            }

            parseCgiOutput(output)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing PHP script", e)
            "<h1>PHP Execution Error</h1><pre>${e.message ?: "Unknown error"}\n\n${e.stackTraceToString().take(500)}</pre>"
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
