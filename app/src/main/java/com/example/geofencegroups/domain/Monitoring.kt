package com.example.geofencegroups.domain

/** What this device can honestly say about its own geofence monitoring. */
enum class MonitoringState {
    /** Permissions granted, geofences registered recently. */
    ACTIVE,
    /** Background (or foreground) location is not granted, so nothing can be detected. */
    PERMISSION_MISSING,
    /** Permissions are fine but the platform can't deliver: location off, Play services missing, or registration failed. */
    UNAVAILABLE,
    /** We registered geofences before, but haven't confirmed them for a long time. Don't claim live monitoring. */
    STALE,
    /** Nothing to monitor yet, or no successful registration recorded on this device. */
    UNKNOWN,
}

data class MonitoringInputs(
    val hasGroupWithBoundary: Boolean,
    val foregroundGranted: Boolean,
    val backgroundGranted: Boolean,
    val locationEnabled: Boolean,
    val playServicesOk: Boolean,
    val lastRegisteredOkMs: Long?,
    val lastRegistrationFailed: Boolean,
    val nowMs: Long,
)

const val MONITORING_STALE_AFTER_MS = 24L * 60 * 60 * 1000

fun evaluateMonitoring(i: MonitoringInputs): MonitoringState = when {
    !i.hasGroupWithBoundary -> MonitoringState.UNKNOWN
    !i.foregroundGranted || !i.backgroundGranted -> MonitoringState.PERMISSION_MISSING
    !i.playServicesOk || !i.locationEnabled || i.lastRegistrationFailed -> MonitoringState.UNAVAILABLE
    i.lastRegisteredOkMs == null -> MonitoringState.UNKNOWN
    i.nowMs - i.lastRegisteredOkMs > MONITORING_STALE_AFTER_MS -> MonitoringState.STALE
    else -> MonitoringState.ACTIVE
}
