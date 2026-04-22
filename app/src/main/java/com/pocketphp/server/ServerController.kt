package com.pocketphp.server

import android.content.Context

/**
 * Controller interface for the PHP HTTP Server.
 * Provides a clean API for the UI to start/stop the server.
 */
class ServerController(private val context: Context) {

    private var server: PhpHttpServer? = null
    private var port: Int = 8080

    fun setPort(newPort: Int) {
        port = newPort
    }

    fun getPort(): Int = port

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

    fun getWwwRoot(): java.io.File {
        return java.io.File(context.filesDir, "www")
    }

    fun getPhpExecutor(): PhpExecutor {
        return server?.getPhpExecutor() ?: PhpExecutor(context)
    }

    fun getRouter(): Router? = server?.getRouter()
}
