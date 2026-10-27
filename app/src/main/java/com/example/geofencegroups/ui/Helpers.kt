package com.example.geofencegroups.ui

import androidx.compose.runtime.*
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.domain.MonitoringState
import kotlinx.coroutines.delay

/** Wall-clock that refreshes every 30 s so relative times don't freeze on screen. */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) { delay(30_000); value = System.currentTimeMillis() }
}

/** Re-evaluates this device's monitoring state on resume and whenever registration results change. */
@Composable
fun rememberMonitoring(c: AppContainer, hasBoundary: Boolean): MonitoringState {
    val snap by c.geofences.store.state.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val now by rememberNow()
    return remember(snap, tick, hasBoundary, now / 60_000) { c.geofences.monitoringState(hasBoundary, snap) }
}
