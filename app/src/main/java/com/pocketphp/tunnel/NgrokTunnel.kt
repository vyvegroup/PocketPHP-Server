package com.pocketphp.tunnel

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ngrok tunnel implementation.
 * Supports both free and authenticated ngrok tunnels.
 */
class NgrokTunnel(
    private val context: Context,
    private val tunnelDir: File
) : TunnelProvider {

    companion object {
        private const val TAG = "NgrokTunnel"
        private const val NGROK_BINARY = "ngrok"
        private const val NGROK_API_URL = "http://127.0.0.1:4040/api/tunnels"
        private const val NGROK_DOWNLOAD_URL = "https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm64.tgz"
    }

    private var process: Process? = null
    private var isRunning = false
    private var logListener: ((String) -> Unit)? = null
    private var stateListener: ((TunnelManager.TunnelState) -> Unit)? = null
    private var urlListener: ((String?) -> Unit)? = null
    private var readerThread: Thread? = null
    private var monitorThread: Thread? = null

    override fun setOnLogListener(listener: (String) -> Unit) { logListener = listener }
    override fun setOnStateListener(listener: (TunnelManager.TunnelState) -> Unit) { stateListener = listener }
    override fun setOnUrlListener(listener: (String?) -> Unit) { urlListener = listener }

    override fun start(localPort: Int) {
        val binary = File(tunnelDir, NGROK_BINARY)
        if (!binary.exists() || !binary.canExecute()) {
            log("Downloading ngrok binary...")
            stateListener?.invoke(TunnelManager.TunnelState.STARTING)
            downloadBinary(binary)
        }

        if (!binary.canExecute()) {
            log("ERROR: Failed to download ngrok")
            stateListener?.invoke(TunnelManager.TunnelState.ERROR)
            return
        }

        log("Starting Ngrok tunnel on port $localPort...")
        stateListener?.invoke(TunnelManager.TunnelState.STARTING)

        try {
            process = ProcessBuilder(
                binary.absolutePath,
                "http",
                "--region=ap",
                "--log=stdout",
                "$localPort"
            ).apply {
                directory(tunnelDir)
                redirectErrorStream(true)
                environment()["HOME"] = tunnelDir.absolutePath
                environment()["NGROK_BOUNDARY"] = tunnelDir.absolutePath
            }.start()

            isRunning = true

            readerThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process?.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val msg = line ?: continue
                        log(msg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading ngrok output", e)
                }
                if (isRunning) {
                    isRunning = false
                    stateListener?.invoke(TunnelManager.TunnelState.DISCONNECTED)
                    urlListener?.invoke(null)
                }
            }
            readerThread?.start()

            // Monitor ngrok API for public URL
            monitorThread = Thread {
                var retries = 0
                while (isRunning && retries < 60) {
                    try {
                        Thread.sleep(2000)
                        val publicUrl = fetchPublicUrl()
                        if (publicUrl != null) {
                            log("Public URL: $publicUrl")
                            urlListener?.invoke(publicUrl)
                            stateListener?.invoke(TunnelManager.TunnelState.CONNECTED)
                            break
                        }
                    } catch (e: Exception) {
                        // API not ready yet
                    }
                    retries++
                }
                if (retries >= 60 && isRunning) {
                    log("WARNING: Could not retrieve public URL from ngrok API")
                }
            }
            monitorThread?.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ngrok", e)
            log("ERROR: ${e.message}")
            stateListener?.invoke(TunnelManager.TunnelState.ERROR)
        }
    }

    override fun stop() {
        isRunning = false
        process?.destroy()
        try { process?.waitFor() } catch (_: Exception) {}
        process = null
        readerThread?.interrupt()
        readerThread?.join(1000)
        readerThread = null
        monitorThread?.interrupt()
        monitorThread?.join(1000)
        monitorThread = null
        stateListener?.invoke(TunnelManager.TunnelState.STOPPED)
        urlListener?.invoke(null)
        log("Ngrok tunnel stopped")
    }

    /**
     * Start with authentication token.
     */
    fun start(localPort: Int, authToken: String?) {
        if (!authToken.isNullOrEmpty()) {
            // Configure ngrok with auth token first
            try {
                val binary = File(tunnelDir, NGROK_BINARY)
                if (binary.exists()) {
                    val configProcess = ProcessBuilder(
                        binary.absolutePath, "config", "add-authtoken", authToken
                    ).apply {
                        directory(tunnelDir)
                        environment()["HOME"] = tunnelDir.absolutePath
                    }.start()
                    configProcess.waitFor()
                    log("Ngrok authtoken configured")
                }
            } catch (e: Exception) {
                log("Failed to configure authtoken: ${e.message}")
            }
        }
        start(localPort)
    }

    private fun fetchPublicUrl(): String? {
        return try {
            val connection = URL(NGROK_API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            // Parse JSON response to find public_url
            val regex = Regex(""public_url"\\s*:\\s*"([^"]+)"")
            regex.find(response)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadBinary(target: File) {
        try {
            val url = URL(NGROK_DOWNLOAD_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            // Download tgz and extract
            val tgzFile = File(tunnelDir, "ngrok.tgz")
            url.openStream().use { input ->
                tgzFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Extract using tar
            val extractProcess = ProcessBuilder("tar", "xzf", tgzFile.absolutePath, "-C", tunnelDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            extractProcess.waitFor()
            tgzFile.delete()

            if (target.exists()) {
                target.setExecutable(true)
                log("ngrok downloaded successfully")
            } else {
                log("ERROR: ngrok binary not found after extraction")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download ngrok", e)
            log("Download failed: ${e.message}")
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        logListener?.invoke(message)
    }
}
