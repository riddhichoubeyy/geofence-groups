package com.example.geofencegroups.push

import com.example.geofencegroups.App
import com.example.geofencegroups.geofence.Work
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

class AppMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val c = (application as App).container
        c.appScope.launch { runCatching { c.devices.register(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "SYNC") Work.enqueueSync(this)
        // Delivered here only while the app is in the foreground; otherwise the system tray shows it.
        message.notification?.let { NotificationHelper.show(this, it.title, it.body) }
    }
}
