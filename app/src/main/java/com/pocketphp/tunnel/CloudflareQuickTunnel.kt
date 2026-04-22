package com.pocketphp.tunnel

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Cloudflare Quick Tunnel implementation using cloudflared.
 * Provides a public HTTPS URL to your local server without any account or token.
 */
class CloudflareQuickTunnel(
    private val context: Context,
    private val tunnelDir: File
) : TunnelProvider {

    companion object {
        private const val TAG = "CloudflareTunnel"
        private const val CF_BINARY = "cloudflared"
        private const val CF_DOWNLOAD_URL = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
    }

    private var process: Process? = null
    private var isRunning = false
    private var logListener: ((String) -> Unit)? = null
    private var stateListener: ((TunnelManager.TunnelState) -> Unit)? = null
    private var urlListener: ((String?) -> Unit)? = null
    private var readerThread: Thread? = null

    override fun setOnLogListener(listener: (String) -> Unit) { logListener = listener }
    override fun setOnStateListener(listener: (TunnelManager.TunnelState) -> Unit) { stateListener = listener }
    override fun setOnUrlListener(listener: (String?) -> Unit) { urlListener = listener }

    override fun start(localPort: Int) {
        val binary = File(tunnelDir, CF_BINARY)
        if (!binary.exists() || !binary.canExecute()) {
            log("Downloading cloudflared binary...")
            stateListener?.invoke(TunnelManager.TunnelState.STARTING)
            downloadBinary(binary)
        }

        if (!binary.canExecute()) {
            log("ERROR: Failed to download cloudflared")
            stateListener?.invoke(TunnelManager.TunnelState.ERROR)
            return
        }

        log("Starting Cloudflare Quick Tunnel on port $localPort...")
        stateListener?.invoke(TunnelManager.TunnelState.STARTING)

        try {
            process = ProcessBuilder(
                binary.absolutePath,
                "tunnel",
                "--url",
                "http://localhost:$localPort",
                "--no-autoupdate"
            ).apply {
                directory(tunnelDir)
                redirectErrorStream(true)
                environment()["HOME"] = tunnelDir.absolutePath
            }.start()

            isRunning = true

            readerThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process?.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val msg = line ?: continue
                        log(msg)

                        // Parse the public URL from cloudflared output
                        if (msg.contains("https://") && msg.contains("trycloudflare.com")) {
                            val urlRegex = Regex("https://[a-zA-Z0-9\\-]+\\.trycloudflare\\.com")
                            val url = urlRegex.find(msg)?.value
                            if (url != null) {
                                log("Public URL: $url")
                                urlListener?.invoke(url)
                                stateListener?.invoke(TunnelManager.TunnelState.CONNECTED)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading tunnel output", e)
                }
                if (isRunning) {
                    isRunning = false
                    stateListener?.invoke(TunnelManager.TunnelState.DISCONNECTED)
                    urlListener?.invoke(null)
                }
            }
            readerThread?.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start cloudflared", e)
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
        readerThread = null
        stateListener?.invoke(TunnelManager.TunnelState.STOPPED)
        urlListener?.invoke(null)
        log("Cloudflare tunnel stopped")
    }

    private fun downloadBinary(target: File) {
        try {
            val url = java.net.URL(CF_DOWNLOAD_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            url.openStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            target.setExecutable(true)
            log("cloudflared downloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download cloudflared", e)
            log("Download failed: ${e.message}")
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        logListener?.invoke(message)
    }
}
