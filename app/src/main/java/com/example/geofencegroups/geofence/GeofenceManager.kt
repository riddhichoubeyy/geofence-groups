package com.example.geofencegroups.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.example.geofencegroups.data.model.Group
import com.example.geofencegroups.domain.MonitoringInputs
import com.example.geofencegroups.domain.MonitoringState
import com.example.geofencegroups.domain.evaluateMonitoring
import com.example.geofencegroups.permissions.Perms
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class GeofenceManager(private val ctx: Context) {
    private val client = LocationServices.getGeofencingClient(ctx)
    val store = MonitoringStore(ctx)

    // Must be mutable: Play Services adds the transition extras to this intent.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, GeofenceReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    /** Replaces all registered geofences with one per group that has a boundary configured. */
    @SuppressLint("MissingPermission")
    suspend fun sync(groups: List<Group>) {
        if (!Perms.allLocation(ctx)) return
        try {
            runCatching { client.removeGeofences(pendingIntent()).await() }
            val fences = groups.mapNotNull { g ->
                val f = g.geofence ?: return@mapNotNull null
                Geofence.Builder()
                    .setRequestId(g.id)
                    .setCircularRegion(f.lat, f.lng, f.radiusM.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }
            if (fences.isEmpty()) { store.recordOk(0); return }
            val req = GeofencingRequest.Builder()
                // Reports ENTER if already inside at registration; the backend records that silently.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(fences).build()
            client.addGeofences(req, pendingIntent()).await()
            store.recordOk(fences.size)
        } catch (e: Exception) {
            store.recordFailure()
            throw e
        }
    }

    suspend fun clear() {
        runCatching { client.removeGeofences(pendingIntent()).await() }
        store.reset()
    }

    /** Snapshot of platform facts; feeds the pure [evaluateMonitoring] function. */
    fun monitoringState(hasBoundary: Boolean, snap: MonitoringStore.Snapshot = store.state.value): MonitoringState {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return evaluateMonitoring(
            MonitoringInputs(
                hasGroupWithBoundary = hasBoundary,
                foregroundGranted = Perms.fine(ctx),
                backgroundGranted = Perms.background(ctx),
                locationEnabled = LocationManagerCompat.isLocationEnabled(lm),
                playServicesOk = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS,
                lastRegisteredOkMs = snap.lastOkMs,
                lastRegistrationFailed = snap.lastFailed,
                nowMs = System.currentTimeMillis(),
            )
        )
    }
}
