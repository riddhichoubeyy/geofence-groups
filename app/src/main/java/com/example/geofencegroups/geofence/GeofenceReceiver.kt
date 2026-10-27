package com.example.geofencegroups.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) { Log.e("GeofenceReceiver", "error ${event.errorCode}"); return }
        val type = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return
        }
        // Receiver has seconds to live: hand off to WorkManager (network constraint + retries).
        event.triggeringGeofences?.forEach { Work.enqueueReport(context, it.requestId, type, event.triggeringLocation) }
    }
}
