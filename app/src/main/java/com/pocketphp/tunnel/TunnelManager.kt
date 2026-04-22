package com.pocketphp.tunnel

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Manages tunnel providers (Cloudflare Quick Tunnel, Ngrok).
 * Handles starting, stopping, and monitoring tunnels.
 */
class TunnelManager(private val context: Context) {

    companion object {
        private const val TAG = "TunnelManager"
        private const val TUNNEL_DIR = "tunnels"
    }

    enum class TunnelType {
        CLOUDFLARE, NGROK
    }

    enum class TunnelState {
        STOPPED, STARTING, CONNECTED, DISCONNECTED, ERROR
    }

    data class TunnelInfo(
        val type: TunnelType,
        val state: TunnelState,
        val publicUrl: String?,
        val localPort: Int,
        val logs: List<String> = emptyList()
    )

    private val tunnelDir = File(context.filesDir, TUNNEL_DIR)
    private var activeTunnel: TunnelProvider? = null
    private var activeType: TunnelType? = null
    private var tunnelState: TunnelState = TunnelState.STOPPED
    private var publicUrl: String? = null
    private val logs = mutableListOf<String>()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var stateListener: ((TunnelState) -> Unit)? = null
    var urlListener: ((String?) -> Unit)? = null
    var logListener: ((String) -> Unit)? = null

    init {
        tunnelDir.mkdirs()
    }

    fun startTunnel(type: TunnelType, localPort: Int, authToken: String? = null) {
        stopTunnel()

        tunnelState = TunnelState.STARTING
        stateListener?.invoke(tunnelState)
        addLog("Starting ${type.name} tunnel on port $localPort...")

        activeType = type
        activeTunnel = when (type) {
            TunnelType.CLOUDFLARE -> CloudflareQuickTunnel(context, tunnelDir)
            TunnelType.NGROK -> NgrokTunnel(context, tunnelDir)
        }

        activeTunnel?.apply {
            setOnLogListener { addLog(it) }
            setOnStateListener { newState ->
                tunnelState = newState
                stateListener?.invoke(newState)
            }
            setOnUrlListener { url ->
                publicUrl = url
                urlListener?.invoke(url)
            }
        }

        scope.launch {
            try {
                val tunnel = activeTunnel ?: return@launch
                when (type) {
                    TunnelType.CLOUDFLARE -> (tunnel as CloudflareQuickTunnel).start(localPort)
                    TunnelType.NGROK -> (tunnel as NgrokTunnel).start(localPort, authToken)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tunnel error", e)
                tunnelState = TunnelState.ERROR
                stateListener?.invoke(tunnelState)
                addLog("ERROR: ${e.message}")
            }
        }
    }

    fun stopTunnel() {
        activeTunnel?.stop()
        activeTunnel = null
        activeType = null
        tunnelState = TunnelState.STOPPED
        publicUrl = null
        stateListener?.invoke(tunnelState)
        urlListener?.invoke(null)
        addLog("Tunnel stopped")
    }

    fun getState(): TunnelState = tunnelState
    fun getPublicUrl(): String? = publicUrl
    fun getType(): TunnelType? = activeType
    fun getLogs(): List<String> = logs.toList()

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$timestamp] $message"
        logs.add(entry)
        if (logs.size > 500) logs.removeFirst()
        Log.d(TAG, entry)
        logListener?.invoke(entry)
    }

    fun destroy() {
        stopTunnel()
        scope.cancel()
    }
}
