package com.example.geofencegroups.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.data.model.Group
import com.example.geofencegroups.domain.MonitoringState
import com.example.geofencegroups.domain.PulseLevel
import com.example.geofencegroups.domain.pulseHeadline
import com.example.geofencegroups.domain.relativeTime
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space

@Composable
fun HomeScreen(c: AppContainer, groups: List<Group>?, loadError: String?, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val vm: GroupsViewModel = viewModel(factory = vmFactory { GroupsViewModel(c) })
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val now by rememberNow()
    val monitoring = rememberMonitoring(c, groups.orEmpty().any { it.geofence != null })
    var dialog by remember { mutableStateOf<String?>(null) } // "create" | "join"
    var text by remember { mutableStateOf("") }

    OrbitScreen("Orbit", trailing = {
        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = Orbit.Forest) }
    }) {
        when {
            groups == null && loadError == null -> LoadingBlock()
            else -> LazyColumn(
                Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.md),
                contentPadding = PaddingValues(start = Space.lg, end = Space.lg, top = Space.sm, bottom = Space.xl + navBarPadding()),
            ) {
                loadError?.let { item { ErrorBanner(it) } }
                message?.let { item { ErrorBanner(it) } }
                val list = groups.orEmpty()
                if (list.isNotEmpty()) item { SummaryCard(list, monitoring) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        PrimaryButton("New group", { text = ""; dialog = "create" }, Modifier.weight(1f), loading = busy)
                        SecondaryButton("Join with code", { text = ""; dialog = "join" }, Modifier.weight(1f))
                    }
                }
                if (list.isEmpty() && groups != null) item {
                    EmptyState("No groups yet", "Create one and share the code, or join a group someone already started.")
                }
                items(list, key = { it.id }) { g -> GroupCard(g, now) { onOpen(g.id) } }
            }
        }
    }

    dialog?.let { kind ->
        val create = kind == "create"
        AlertDialog(
            onDismissRequest = { dialog = null }, containerColor = Orbit.Surface,
            title = { Text(if (create) "Name your group" else "Enter invite code", style = MaterialTheme.typography.titleLarge) },
            text = {
                OrbitField(text, { text = if (create) it.take(60) else it.filter { ch -> ch.isLetterOrDigit() }.take(8).uppercase() },
                    if (create) "Group name" else "Invite code",
                    keyboardOptions = KeyboardOptions(capitalization = if (create) KeyboardCapitalization.Words else KeyboardCapitalization.Characters))
            },
            confirmButton = {
                QuietButton(if (create) "Create" else "Join", onClick = {
                    val v = text.trim(); dialog = null
                    if (v.isNotEmpty()) { if (create) vm.create(v) { onOpen(it) } else vm.join(v) { onOpen(it) } }
                })
            },
            dismissButton = { QuietButton("Cancel", { dialog = null }) },
        )
    }
}

@Composable
private fun SummaryCard(groups: List<Group>, monitoring: MonitoringState) {
    val outside = groups.sumOf { it.pulse.outside }
    OrbitCard(tint = if (outside > 0) Orbit.EmberSoft else Orbit.Surface) {
        Text("RIGHT NOW", style = MaterialTheme.typography.labelMedium, color = Orbit.ForestSoft)
        Spacer(Modifier.height(Space.xs))
        Text(
            when {
                groups.none { it.geofence != null } -> "No boundaries set yet"
                outside == 0 -> "Nobody is outside a boundary"
                outside == 1 -> "1 member is outside a boundary"
                else -> "$outside members are outside a boundary"
            }, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            StatusPill("${groups.size} ${if (groups.size == 1) "group" else "groups"}", Tone.Neutral)
            when (monitoring) {
                MonitoringState.ACTIVE -> StatusPill("This phone: monitoring", Tone.Good)
                MonitoringState.PERMISSION_MISSING -> StatusPill("Permission needed", Tone.Warn)
                MonitoringState.UNAVAILABLE -> StatusPill("Monitoring unavailable", Tone.Alert)
                MonitoringState.STALE -> StatusPill("Status out of date", Tone.Warn)
                MonitoringState.UNKNOWN -> StatusPill("Monitoring not confirmed", Tone.Neutral)
            }
        }
    }
}

@Composable
private fun GroupCard(g: Group, now: Long, onClick: () -> Unit) {
    val h = pulseHeadline(g.pulse, g.geofence != null)
    OrbitCard(onClick = onClick) {
        Text(g.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Space.xs))
        Text("${g.memberUids.size} ${if (g.memberUids.size == 1) "member" else "members"}",
            style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
        Spacer(Modifier.height(Space.sm))
        StatusPill(h.title, when (h.level) {
            PulseLevel.SOMEONE_OUTSIDE -> Tone.Alert
            PulseLevel.ALL_INSIDE -> Tone.Good
            else -> Tone.Neutral
        })
        val at = g.pulse.lastEventAtMs
        if (at != null && g.pulse.lastEventName != null) {
            Spacer(Modifier.height(Space.sm))
            Text("${g.pulse.lastEventName} ${if (g.pulse.lastEventType == "ENTER") "came back" else "left"} · ${relativeTime(at, now)}",
                style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
        }
    }
}
