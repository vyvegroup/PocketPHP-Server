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
 * Supports ARM64 and ARMv7 architectures.
 * Extracts PHP binary and libraries to the app's files directory.
 */
class PhpInstaller(private val context: Context) {

    companion object {
        private const val TAG = "PhpInstaller"
        const val PHP_DIR_NAME = "php"
        private const val PHP_BIN_NAME = "php"
        private const val PHP_LIB_DIR = "lib"
    }

    val phpDir = File(context.filesDir, PHP_DIR_NAME)
    private val phpBinDir = File(phpDir, "bin")
    private val phpLibDir = File(phpDir, PHP_LIB_DIR)
    val phpBinary = File(phpBinDir, PHP_BIN_NAME)

    /**
     * Get the device architecture string matching our asset directories.
     */
    fun getDeviceArch(): String {
        val abis = Build.SUPPORTED_ABIS
        return when (abis.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "arm64"  // Fallback: try ARM64 binary
            "x86" -> "arm"      // Fallback: try ARM binary
            else -> "arm64"     // Default to ARM64
        }
    }

    /**
     * Get the Termux-compatible architecture string.
     */
    fun getTermuxArch(): String {
        return when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "aarch64"
            "x86" -> "arm"
            else -> "aarch64"
        }
    }

    /**
     * Check if PHP is already installed and the binary exists.
     */
    fun isInstalled(): Boolean {
        return phpBinary.exists() && phpBinary.canExecute()
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
                val termuxArch = getTermuxArch()
                Log.i(TAG, "Installing PHP for device arch: $arch (Termux: $termuxArch)")
                onProgress(2, "Detected architecture: $arch")

                // Prepare directories
                onProgress(5, "Creating directories...")
                phpDir.mkdirs()
                phpBinDir.mkdirs()
                phpLibDir.mkdirs()

                // Try architecture-specific extraction
                val assetBinPath = "php/$arch/bin/php"
                val assetLibDir = "php/$arch/lib"

                Log.i(TAG, "Looking for assets at: $assetBinPath")
                onProgress(8, "Checking bundled PHP $arch...")

                // Check if the PHP binary exists in assets for this architecture
                val assetList = try {
                    context.assets.list("php/$arch/bin")?.toList() ?: emptyList()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot list assets/php/$arch/bin: ${e.message}")
                    emptyList()
                }

                if (assetList.contains("php")) {
                    onProgress(10, "Extracting PHP 8.3 binary...")
                    extractFromAssets(assetBinPath, phpBinary)
                    phpBinary.setExecutable(true)

                    val phpSize = phpBinary.length() / 1024
                    Log.i(TAG, "PHP binary extracted: ${phpSize}KB")
                    onProgress(30, "PHP binary extracted (${phpSize}KB)")

                    // Extract libraries
                    onProgress(35, "Extracting PHP libraries...")
                    val libFiles = try {
                        context.assets.list(assetLibDir)?.toList() ?: emptyList()
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot list asset libs: ${e.message}")
                        emptyList()
                    }

                    val totalLibs = libFiles.size
                    if (totalLibs == 0) {
                        Log.w(TAG, "No library files found in assets/$assetLibDir")
                    }

                    var extractedLibs = 0
                    for (libName in libFiles) {
                        if (libName.endsWith(".so") || libName.contains(".so.")) {
                            extractFromAssets("$assetLibDir/$libName", File(phpLibDir, libName))
                            extractedLibs++
                        }
                        if (extractedLibs % 10 == 0) {
                            val progress = 35 + ((extractedLibs.toFloat() / maxOf(totalLibs, 1)) * 35).toInt()
                            onProgress(progress, "Extracting libraries ($extractedLibs/$totalLibs)")
                        }
                    }

                    val libProgress = 35 + 35
                    onProgress(libProgress, "Libraries extracted ($extractedLibs files)")

                    // Write PHP configuration
                    onProgress(75, "Writing PHP configuration...")
                    writeDefaultIni()

                    // Create sessions directory
                    File(phpDir, "sessions").mkdirs()

                    // Verify PHP installation
                    onProgress(85, "Verifying PHP installation...")
                    val version = verifyInstallation()

                    if (version != null) {
                        Log.i(TAG, "PHP installed successfully: $version")
                        onProgress(100, "PHP $version installed successfully!")
                        true
                    } else {
                        Log.e(TAG, "PHP verification failed")
                        onProgress(-1, "PHP verification failed. Binary may not be compatible with this device ($arch).")
                        false
                    }
                } else {
                    // No bundled PHP for this architecture
                    Log.e(TAG, "No PHP binary found for architecture: $arch")
                    Log.e(TAG, "Available assets: php/arm64/bin/ = ${try { context.assets.list("php/arm64/bin")?.toList() } catch(e: Exception) { null }}, php/arm/bin/ = ${try { context.assets.list("php/arm/bin")?.toList() } catch(e: Exception) { null }}")
                    onProgress(-1, "PHP binary for '$arch' is not bundled in this APK. Your device architecture may not be supported. Device: ${Build.SUPPORTED_ABIS.firstOrNull()}")
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
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
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
                max_input_time = 60

                ; Extensions
                extension_dir = "${phpLibDir.absolutePath}"

                ; Session
                session.save_path = "${phpDir.absolutePath}/sessions"
                session.save_handler = files

                ; CGI
                cgi.force_redirect = 0
                cgi.fix_pathinfo = 1

                ; Security
                open_basedir = "${context.filesDir.absolutePath}"
                allow_url_fopen = On
                allow_url_include = Off

                ; Date
                date.timezone = UTC

                ; File uploads
                upload_tmp_dir = "${phpDir.absolutePath}/tmp"
            """.trimIndent())
            File(phpDir, "tmp").mkdirs()
            File(phpDir, "sessions").mkdirs()
        }
    }

    /**
     * Verify PHP installation by running php -v.
     */
    private fun verifyInstallation(): String? {
        return try {
            if (!phpBinary.exists()) {
                Log.e(TAG, "PHP binary not found at ${phpBinary.absolutePath}")
                return null
            }

            val process = ProcessBuilder(phpBinary.absolutePath, "-v")
                .apply {
                    directory(phpDir)
                    environment()["LD_LIBRARY_PATH"] = phpLibDir.absolutePath
                    redirectErrorStream(true)
                }
                .start()

            // Read output with timeout
            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor()

            if (finished == 0 && output.contains("PHP")) {
                val versionLine = output.lines().firstOrNull { it.contains("PHP") }
                Log.i(TAG, "PHP version: $versionLine")
                versionLine
            } else {
                Log.e(TAG, "PHP -v failed (exit=$finished): $output")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify PHP installation", e)
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
     * Get the library directory path.
     */
    fun getLibDir(): File = phpLibDir

    /**
     * Get detailed diagnostics info.
     */
    fun getDiagnostics(): String {
        val sb = StringBuilder()
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("Arch: ${getDeviceArch()}")
        sb.appendLine("---")
        sb.appendLine("PHP binary: ${phpBinary.absolutePath}")
        sb.appendLine("  Exists: ${phpBinary.exists()}")
        sb.appendLine("  Executable: ${phpBinary.canExecute()}")
        sb.appendLine("  Size: ${if (phpBinary.exists()) "${phpBinary.length() / 1024}KB" else "N/A"}")
        sb.appendLine("Lib dir: ${phpLibDir.absolutePath}")
        sb.appendLine("  Exists: ${phpLibDir.exists()}")
        val libCount = if (phpLibDir.exists()) phpLibDir.listFiles()?.count { it.name.endsWith(".so") || it.name.contains(".so.") } ?: 0 else 0
        sb.appendLine("  .so files: $libCount")
        sb.appendLine("---")
        sb.appendLine("Assets check:")

        // Check what's available in assets
        try {
            val phpAssets = context.assets.list("php")?.toList() ?: emptyList()
            sb.appendLine("  php/ contents: $phpAssets")
            for (archDir in phpAssets) {
                val binFiles = context.assets.list("php/$archDir/bin")?.toList() ?: emptyList()
                val libFiles = context.assets.list("php/$archDir/lib")?.toList() ?: emptyList()
                sb.appendLine("  php/$archDir/bin/: $binFiles")
                sb.appendLine("  php/$archDir/lib/: ${libFiles.size} files")
            }
        } catch (e: Exception) {
            sb.appendLine("  Error listing assets: ${e.message}")
        }

        return sb.toString()
    }

    /**
     * Uninstall PHP by deleting all files.
     */
    fun uninstall() {
        phpDir.deleteRecursively()
    }
}
