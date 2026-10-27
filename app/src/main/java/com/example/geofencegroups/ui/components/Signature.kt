package com.example.geofencegroups.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.geofencegroups.data.model.GeoEvent
import com.example.geofencegroups.domain.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space

/**
 * Group Pulse: one glance at who is inside, who is outside and who hasn't reported.
 * It reflects the last crossing each member reported, never live location, and says so.
 */
@Composable
fun GroupPulse(pulse: Pulse, hasBoundary: Boolean, nowMs: Long, modifier: Modifier = Modifier) {
    val h = pulseHeadline(pulse, hasBoundary)
    val tint = when (h.level) {
        PulseLevel.SOMEONE_OUTSIDE -> Orbit.EmberSoft
        PulseLevel.ALL_INSIDE -> Orbit.LimeSoft
        else -> Orbit.Surface
    }
    OrbitCard(modifier, tint = tint) {
        Text("GROUP PULSE", style = MaterialTheme.typography.labelMedium, color = Orbit.ForestSoft)
        Spacer(Modifier.height(Space.xs))
        Text(h.title, style = MaterialTheme.typography.headlineSmall)
        Text(h.detail, style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
        val total = (pulse.inside + pulse.outside + pulse.unknown).coerceAtLeast(1)
        if (hasBoundary) {
            Spacer(Modifier.height(Space.md))
            PulseBar(pulse.inside / total.toFloat(), pulse.outside / total.toFloat(), pulse.unknown / total.toFloat())
        }
        val last = pulse.lastEventAtMs
        if (last != null && pulse.lastEventName != null) {
            Spacer(Modifier.height(Space.md))
            val verb = if (pulse.lastEventType == "ENTER") "came back" else "left"
            Text("Latest: ${pulse.lastEventName} $verb · ${relativeTime(last, nowMs)}", style = MaterialTheme.typography.bodyMedium)
        }
        if (hasBoundary) {
            Spacer(Modifier.height(Space.xs))
            Text("Based on each member's last crossing, not live location.", style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
        }
    }
}

@Composable
private fun PulseBar(inside: Float, outside: Float, unknown: Float) {
    val i by animateFloatAsState(inside, tween(450), label = "inside")
    val o by animateFloatAsState(outside, tween(450), label = "outside")
    val u by animateFloatAsState(unknown, tween(450), label = "unknown")
    Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Orbit.Sand)) {
        if (i > 0f) Box(Modifier.weight(i).fillMaxHeight().background(Orbit.Moss))
        if (o > 0f) Box(Modifier.weight(o).fillMaxHeight().background(Orbit.Ember))
        if (u > 0f) Box(Modifier.weight(u).fillMaxHeight().background(Orbit.Line))
    }
}

/** This device's honest monitoring state, with the next step when something is wrong. */
@Composable
fun MonitoringBanner(state: MonitoringState, onFix: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (tone, title, body, action) = when (state) {
        MonitoringState.ACTIVE -> Quad(Tone.Good, "Monitoring active", "This phone is watching the boundary in the background.", null)
        MonitoringState.PERMISSION_MISSING -> Quad(Tone.Warn, "Location permission needed",
            "Orbit can't detect when you cross the boundary until location is set to “Allow all the time”.", "Fix in Settings")
        MonitoringState.UNAVAILABLE -> Quad(Tone.Alert, "Monitoring unavailable",
            "Location is switched off, Google Play services is missing, or the boundary couldn't be registered.", "Check settings")
        MonitoringState.STALE -> Quad(Tone.Warn, "Status out of date",
            "This phone hasn't confirmed monitoring in over a day. Open Orbit to refresh it.", "Check settings")
        MonitoringState.UNKNOWN -> Quad(Tone.Neutral, "Monitoring not confirmed yet",
            "Nothing to report until a boundary is set and this phone registers it.", null)
    }
    val (bg, fg, icon) = when (tone) {
        Tone.Good -> Triple(Orbit.LimeSoft, Orbit.Forest, Icons.Filled.Check)
        Tone.Alert -> Triple(Orbit.EmberSoft, Orbit.Ember, Icons.Filled.Warning)
        Tone.Warn -> Triple(Orbit.AmberSoft, Orbit.Amber, Icons.Filled.Lock)
        Tone.Neutral -> Triple(Orbit.Sand, Orbit.ForestSoft, Icons.Filled.Info)
    }
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(bg).padding(Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = fg)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = fg)
            if (action != null && onFix != null) QuietButton(action, onFix)
        }
    }
}

private data class Quad(val tone: Tone, val title: String, val body: String, val action: String?)

/** Activity timeline grouped by day; exits are emphasised because they are what the group cares about. */
@Composable
fun ActivityTimeline(events: List<GeoEvent>, nowMs: Long, modifier: Modifier = Modifier) {
    if (events.isEmpty()) {
        Text("No crossings yet. When someone leaves or returns, it shows up here.",
            style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft, modifier = modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        var lastDay: String? = null
        events.forEach { e ->
            val t = e.timeMs
            val day = t?.let { dayLabel(it, nowMs) } ?: "Today"
            if (day != lastDay) {
                Text(day.uppercase(), style = MaterialTheme.typography.labelMedium, color = Orbit.ForestSoft,
                    modifier = Modifier.padding(top = Space.sm))
                lastDay = day
            }
            TimelineRow(e, nowMs)
        }
    }
}

@Composable
private fun TimelineRow(e: GeoEvent, nowMs: Long) {
    val exit = e.type == "EXIT"
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (exit) 12.dp else 8.dp).clip(CircleShape).background(if (exit) Orbit.Ember else Orbit.Moss))
        Column(Modifier.weight(1f)) {
            Text(if (exit) "${e.displayName} left the boundary" else "${e.displayName} came back",
                style = if (exit) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = if (exit) Orbit.Forest else Orbit.ForestSoft)
        }
        Text(e.timeMs?.let { "${clockTime(it)} · ${relativeTime(it, nowMs)}" } ?: "…",
            style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
    }
}
