package com.phpserver.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PhpServerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serverChannel = NotificationChannel(
                CHANNEL_SERVER,
                "PHP Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows PHP server status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serverChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVER = "php_server_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: PhpServerApp
            private set
    }
}
