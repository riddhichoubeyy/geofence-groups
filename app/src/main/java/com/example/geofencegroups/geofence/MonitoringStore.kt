package com.example.geofencegroups.geofence

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists the outcome of the last geofence registration so status survives process restarts. */
class MonitoringStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("orbit_monitoring", Context.MODE_PRIVATE)

    data class Snapshot(val lastOkMs: Long?, val lastFailed: Boolean, val registered: Int)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Snapshot> = _state

    private fun read() = Snapshot(
        lastOkMs = prefs.getLong("ok_at", 0L).takeIf { it > 0 },
        lastFailed = prefs.getBoolean("failed", false),
        registered = prefs.getInt("count", 0),
    )

    fun recordOk(count: Int) {
        prefs.edit().putLong("ok_at", System.currentTimeMillis()).putBoolean("failed", false).putInt("count", count).apply()
        _state.value = read()
    }

    fun recordFailure() {
        prefs.edit().putBoolean("failed", true).apply()
        _state.value = read()
    }

    fun reset() {
        prefs.edit().clear().apply()
        _state.value = read()
    }
}
