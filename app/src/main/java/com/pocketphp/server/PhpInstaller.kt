package com.pocketphp.server

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles PHP 8.3 installation from bundled assets.
 * On first run, extracts the PHP binary and libraries to the app's files directory.
 */
class PhpInstaller(private val context: Context) {

    companion object {
        private const val TAG = "PhpInstaller"
        const val PHP_DIR_NAME = "php"
        private const val PHP_BIN_NAME = "php"
        private const val PHP_LIB_DIR = "lib"
        private const val ASSET_PHP_BIN = "php/bin/php"
        private const val ASSET_PHP_LIB_DIR = "php/lib"
    }

    val phpDir = File(context.filesDir, PHP_DIR_NAME)
    private val phpBinDir = File(phpDir, "bin")
    private val phpLibDir = File(phpDir, PHP_LIB_DIR)
    val phpBinary = File(phpBinDir, PHP_BIN_NAME)

    /**
     * Check if PHP is already installed.
     */
    fun isInstalled(): Boolean {
        return phpBinary.exists() && phpBinary.canExecute()
    }

    /**
     * Get the device architecture string.
     */
    fun getDeviceArch(): String {
        return when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "x86"
            else -> "arm64"
        }
    }

    /**
     * Install PHP from bundled assets. Runs on IO dispatcher.
     * @param onProgress callback with progress percentage (0-100) and message
     * @return true if installation succeeded
     */
    suspend fun install(onProgress: (Int, String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isInstalled()) {
                    onProgress(100, "PHP already installed")
                    return@withContext true
                }

                val arch = getDeviceArch()
                Log.i(TAG, "Installing PHP for architecture: $arch")

                onProgress(5, "Preparing PHP $arch directories...")
                phpDir.mkdirs()
                phpBinDir.mkdirs()
                phpLibDir.mkdirs()

                onProgress(10, "Extracting PHP 8.3 binary...")
                extractFromAssets(ASSET_PHP_BIN, phpBinary)
                phpBinary.setExecutable(true)
                onProgress(40, "PHP binary extracted")

                onProgress(45, "Extracting PHP libraries...")
                val assetManager = context.assets
                val libs = assetManager.list(ASSET_PHP_LIB_DIR) ?: emptyArray()
                val totalLibs = libs.size
                libs.forEachIndexed { index, libName ->
                    extractFromAssets("$ASSET_PHP_LIB_DIR/$libName", File(phpLibDir, libName))
                    val progress = 45 + ((index.toFloat() / totalLibs) * 40).toInt()
                    onProgress(progress, "Extracting libraries ($index/$totalLibs)")
                }
                onProgress(85, "Libraries extracted ($totalLibs files)")

                onProgress(90, "Writing PHP configuration...")
                writeDefaultIni()
                onProgress(95, "Verifying PHP installation...")
                val version = verifyInstallation()
                if (version != null) {
                    Log.i(TAG, "PHP installed: $version")
                    onProgress(100, "PHP $version installed successfully")
                    true
                } else {
                    Log.e(TAG, "PHP verification failed")
                    onProgress(-1, "PHP verification failed - binary may not be compatible with this device")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "PHP installation failed", e)
                onProgress(-1, "Error: ${e.message}")
                false
            }
        }
    }

    /**
     * Extract a file from assets to a destination file.
     */
    private fun extractFromAssets(assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            // Make binaries executable
            if (!destFile.name.contains(".so") && !destFile.extension.contains("so")) {
                destFile.setExecutable(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract $assetPath: ${e.message}")
        }
    }

    /**
     * Write a default php.ini configuration.
     */
    private fun writeDefaultIni() {
        val iniFile = File(phpDir, "php.ini")
        if (!iniFile.exists()) {
            iniFile.writeText("""
                ; PocketPHP PHP 8.3 Configuration
                [PHP]
                engine = On
                short_open_tag = On
                display_errors = On
                display_startup_errors = On
                error_reporting = E_ALL
                log_errors = Off
                html_errors = On

                ; Memory
                memory_limit = 128M
                post_max_size = 100M
                upload_max_filesize = 100M
                max_execution_time = 30

                ; Extensions
                extension_dir = "${phpLibDir.absolutePath}"

                ; Session
                session.save_path = "${phpDir.absolutePath}/sessions"

                ; CGI
                cgi.force_redirect = 0
                cgi.fix_pathinfo = 1

                ; Security
                open_basedir = "${context.filesDir.absolutePath}"
                allow_url_fopen = On
                allow_url_include = Off

                ; Date
                date.timezone = UTC
            """.trimIndent())
            File(phpDir, "sessions").mkdirs()
        }
    }

    /**
     * Verify PHP installation by running php -v.
     */
    private fun verifyInstallation(): String? {
        return try {
            if (!phpBinary.exists()) return null

            val process = ProcessBuilder(phpBinary.absolutePath, "-v")
                .apply {
                    environment()["LD_LIBRARY_PATH"] = phpLibDir.absolutePath
                    redirectErrorStream(true)
                }
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.contains("PHP")) {
                output.lines().firstOrNull { it.contains("PHP") }
            } else {
                Log.e(TAG, "PHP -v returned code $exitCode: $output")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify PHP", e)
            null
        }
    }

    /**
     * Get PHP version string if installed.
     */
    fun getPhpVersion(): String? {
        if (!isInstalled()) return null
        return verifyInstallation()
    }

    /**
     * Get the library directory path for LD_LIBRARY_PATH.
     */
    fun getLibDir(): File = phpLibDir

    /**
     * Uninstall PHP by deleting all files.
     */
    fun uninstall() {
        phpDir.deleteRecursively()
    }
}
