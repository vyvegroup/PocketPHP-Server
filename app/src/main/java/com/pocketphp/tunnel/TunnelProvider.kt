package com.pocketphp.tunnel

import java.io.File

/**
 * Interface for tunnel providers.
 */
interface TunnelProvider {
    fun start(localPort: Int)
    fun stop()
    fun setOnLogListener(listener: (String) -> Unit)
    fun setOnStateListener(listener: (TunnelManager.TunnelState) -> Unit)
    fun setOnUrlListener(listener: (String?) -> Unit)
}
