package com.phpserver.android.server

import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.phpserver.android.PhpServerApp
import com.phpserver.android.R
import kotlinx.coroutines.*

class ServerService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webServer: WebServer? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startServer(intent.getIntExtra("port", 8080))
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startServer(port: Int) {
        if (webServer?.isAlive() == true) return

        startForegroundNotification(port, "Starting...")
        serviceScope.launch {
            try {
                webServer = WebServer(port).also {
                    it.start()
                    startForegroundNotification(port, "Running")
                    ServerState.isRunning = true
                    ServerState.port = port
                }
            } catch (e: Exception) {
                ServerState.error = e.message ?: "Unknown error"
                ServerState.isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            try {
                webServer?.stop()
            } catch (_: Exception) {}
            webServer = null
            ServerState.isRunning = false
            ServerState.port = 0
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundNotification(port: Int, status: String) {
        val notification = NotificationCompat.Builder(this, PhpServerApp.CHANNEL_SERVER)
            .setContentTitle("PHP Server")
            .setContentText("Port $port - $status")
            .setSmallIcon(R.drawable.ic_server)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(PhpServerApp.NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(PhpServerApp.NOTIFICATION_ID, notification)
        }
    }

    fun getServerStatus(): ServerStatus {
        return ServerStatus(
            isRunning = webServer?.isAlive() == true,
            port = ServerState.port,
            uptime = webServer?.getUptime() ?: 0,
            requestCount = webServer?.getRequestCount() ?: 0
        )
    }

    companion object {
        const val ACTION_START = "com.phpserver.android.START_SERVER"
        const val ACTION_STOP = "com.phpserver.android.STOP_SERVER"
    }
}

object ServerState {
    var isRunning = false
    var port = 8080
    var error: String? = null
    val logs = mutableListOf<String>()
    var phpVersion: String = "8.2.13"
    var documentRoot: String = "/data/local/php/www"
}

data class ServerStatus(
    val isRunning: Boolean,
    val port: Int,
    val uptime: Long,
    val requestCount: Int
)
