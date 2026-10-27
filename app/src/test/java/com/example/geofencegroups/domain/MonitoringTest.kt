package com.example.geofencegroups.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringTest {
    private val now = 10_000_000_000L
    private fun inputs(
        hasBoundary: Boolean = true, fg: Boolean = true, bg: Boolean = true, loc: Boolean = true,
        play: Boolean = true, lastOk: Long? = now - 1000, failed: Boolean = false,
    ) = MonitoringInputs(hasBoundary, fg, bg, loc, play, lastOk, failed, now)

    @Test fun activeWhenEverythingIsHealthyAndRecent() =
        assertEquals(MonitoringState.ACTIVE, evaluateMonitoring(inputs()))

    @Test fun unknownWhenNoBoundaryExists() =
        assertEquals(MonitoringState.UNKNOWN, evaluateMonitoring(inputs(hasBoundary = false)))

    @Test fun permissionMissingWithoutBackgroundLocation() =
        assertEquals(MonitoringState.PERMISSION_MISSING, evaluateMonitoring(inputs(bg = false)))

    @Test fun permissionMissingWithoutForegroundLocation() =
        assertEquals(MonitoringState.PERMISSION_MISSING, evaluateMonitoring(inputs(fg = false)))

    @Test fun permissionBeatsUnavailable() =
        assertEquals(MonitoringState.PERMISSION_MISSING, evaluateMonitoring(inputs(bg = false, loc = false)))

    @Test fun unavailableWhenLocationServicesOff() =
        assertEquals(MonitoringState.UNAVAILABLE, evaluateMonitoring(inputs(loc = false)))

    @Test fun unavailableWithoutPlayServices() =
        assertEquals(MonitoringState.UNAVAILABLE, evaluateMonitoring(inputs(play = false)))

    @Test fun unavailableWhenLastRegistrationFailed() =
        assertEquals(MonitoringState.UNAVAILABLE, evaluateMonitoring(inputs(failed = true)))

    @Test fun unknownWhenNeverRegistered() =
        assertEquals(MonitoringState.UNKNOWN, evaluateMonitoring(inputs(lastOk = null)))

    @Test fun staleWhenRegistrationIsOlderThanADay() =
        assertEquals(MonitoringState.STALE, evaluateMonitoring(inputs(lastOk = now - MONITORING_STALE_AFTER_MS - 1)))

    @Test fun notStaleExactlyAtTheLimit() =
        assertEquals(MonitoringState.ACTIVE, evaluateMonitoring(inputs(lastOk = now - MONITORING_STALE_AFTER_MS)))
}
