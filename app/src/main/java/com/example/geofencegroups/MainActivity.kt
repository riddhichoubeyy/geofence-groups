package com.example.geofencegroups

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val deepLinkGroup = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readLink(intent)
        val container = (application as App).container
        setContent { com.example.geofencegroups.ui.Root(container, deepLinkGroup) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readLink(intent)
    }

    // FCM puts the data payload into the launch intent when the user taps a notification shown by the system.
    private fun readLink(i: Intent?) { deepLinkGroup.value = i?.getStringExtra("groupId") }
}
