package com.pocketphp.server

import android.content.Context
import java.io.File

/**
 * Controller for the PHP HTTP Server.
 * Manages server lifecycle, PHP installation, and configuration.
 */
class ServerController(private val context: Context) {

    private var server: PhpHttpServer? = null
    private var port: Int = 8080
    private val phpExecutor = PhpExecutor(context)

    fun setPort(newPort: Int) { port = newPort }
    fun getPort(): Int = port

    /**
     * Check if PHP is installed.
     */
    fun isPhpInstalled(): Boolean = phpExecutor.isPhpInstalled()

    /**
     * Get the PHP installer for UI operations.
     */
    fun getPhpInstaller(): PhpInstaller = phpExecutor.getInstaller()

    /**
     * Get PHP version string.
     */
    fun getPhpVersion(): String? = phpExecutor.getPhpVersion()

    fun start(): Boolean {
        if (getStatus() == PhpHttpServer.ServerStatus.RUNNING) return true
        stop()
        server = PhpHttpServer(context, port)
        server?.start()
        return server?.getStatus() == PhpHttpServer.ServerStatus.RUNNING
    }

    fun stop() {
        server?.stop()
        server = null
    }

    fun getStatus(): PhpHttpServer.ServerStatus = server?.getStatus() ?: PhpHttpServer.ServerStatus.STOPPED
    fun getServer(): PhpHttpServer? = server

    fun setLogCallback(callback: (String) -> Unit) {
        server?.logCallback = callback
    }

    fun getWwwRoot(): File = File(context.filesDir, "www")
    fun getPhpExecutor(): PhpExecutor = phpExecutor
    fun getRouter(): Router? = server?.getRouter()
}
