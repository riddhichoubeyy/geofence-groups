package com.example.geofencegroups.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.domain.PresenceState
import com.example.geofencegroups.domain.presenceLabel
import com.example.geofencegroups.domain.presenceOf
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space
import com.google.android.gms.maps.model.LatLng
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

@Composable
fun GroupDetailScreen(
    c: AppContainer, myUid: String, groupId: String,
    onBack: () -> Unit, onEditBoundary: () -> Unit, onSettings: () -> Unit,
) {
    val vm: GroupDetailViewModel = viewModel(factory = vmFactory { GroupDetailViewModel(c, groupId) })
    val group by vm.group.collectAsState()
    val members by vm.members.collectAsState()
    val events by vm.events.collectAsState()
    val message by vm.message.collectAsState()
    val busy by vm.busy.collectAsState()
    val inviteCode by vm.inviteCode.collectAsState()
    val inviteCodeLoading by vm.inviteCodeLoading.collectAsState()
    val now by rememberNow()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var confirmLeave by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    val g = group
    val monitoring = rememberMonitoring(c, g?.geofence != null)
    val myRole = members.firstOrNull { it.uid == myUid }?.role
    val canEdit = myRole == "owner" || myRole == "admin"
    LaunchedEffect(groupId, canEdit) { if (canEdit) vm.loadInviteCode() }

    OrbitScreen(g?.name ?: "Group", onBack = onBack) {
        if (g == null) { LoadingBlock(); return@OrbitScreen }
        LazyColumn(
            Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.md),
            contentPadding = PaddingValues(start = Space.lg, end = Space.lg, top = Space.sm, bottom = Space.xxl + navBarPadding()),
        ) {
            message?.let { item { ErrorBanner(it) } }

            item {
                val f = g.geofence
                if (f == null) {
                    OrbitCard {
                        Text("No boundary yet", style = MaterialTheme.typography.titleLarge)
                        Text(if (canEdit) "Pick a place on the map and a radius. Members are watched once it's set."
                            else "An owner or admin needs to set the boundary.",
                            style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
                        if (canEdit) { Spacer(Modifier.height(Space.md)); PrimaryButton("Set boundary", onEditBoundary) }
                    }
                } else {
                    BoundaryMap(LatLng(f.lat, f.lng), f.radiusM, Modifier.fillMaxWidth().height(200.dp), cameraKey = "${f.lat},${f.lng},${f.radiusM}")
                    Row(Modifier.fillMaxWidth().padding(top = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("Radius ${f.radiusM.toInt()} m", Tone.Neutral)
                        Spacer(Modifier.weight(1f))
                        if (canEdit) QuietButton("Edit boundary", onEditBoundary)
                    }
                }
            }

            item { GroupPulse(g.pulse, g.geofence != null, now) }
            item { MonitoringBanner(monitoring, onFix = onSettings) }

            if (canEdit) item {
                OrbitCard {
                    Text("INVITE CODE", style = MaterialTheme.typography.labelMedium, color = Orbit.ForestSoft)
                    when {
                        inviteCode != null -> Text(inviteCode!!, style = MaterialTheme.typography.displayMedium)
                        inviteCodeLoading -> Text("Loading invite code…", style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
                        else -> QuietButton("Retry loading code", vm::loadInviteCode)
                    }
                    if (inviteCode != null) Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        SecondaryButton(if (copied) "Copied" else "Copy", {
                            clipboard.setText(AnnotatedString(inviteCode!!)); copied = true
                        }, Modifier.weight(1f))
                        PrimaryButton("Share", {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Join “${g.name}” on Orbit with code ${inviteCode!!}")
                            }
                            ctx.startActivity(Intent.createChooser(send, "Share invite code"))
                        }, Modifier.weight(1f))
                    }
                }
            }

            item { SectionTitle("Members") }
            items(members, key = { "m-" + it.uid }) { m ->
                val p = presenceOf(m.insideGeofence, m.lastTransitionMs, now)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Avatar(m.displayName, tone = when {
                        p.stale -> Tone.Warn
                        p.state == PresenceState.INSIDE -> Tone.Good
                        p.state == PresenceState.OUTSIDE -> Tone.Alert
                        else -> Tone.Neutral
                    })
                    Column(Modifier.weight(1f)) {
                        Text(m.displayName + if (m.uid == myUid) " (you)" else "", style = MaterialTheme.typography.titleMedium)
                        Text(presenceLabel(p, now), style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
                    }
                    if (m.role != "member") StatusPill(m.role, Tone.Neutral)
                    if (myRole == "owner" && m.role != "owner") {
                        QuietButton(if (m.role == "admin") "Remove admin" else "Make admin",
                            { vm.setRole(m.uid, if (m.role == "admin") "member" else "admin") })
                    }
                }
            }

            item { SectionTitle("Activity") }
            item { ActivityTimeline(events, now) }

            if (myRole != null && myRole != "owner") item {
                QuietButton("Leave group", { confirmLeave = true }, destructive = true)
            }
        }
    }

    if (confirmLeave) ConfirmDialog(
        title = "Leave this group?", destructive = true, confirmLabel = if (busy) "Leaving…" else "Leave",
        body = "You'll stop being watched and stop getting alerts. You can rejoin with the invite code.",
        onConfirm = { confirmLeave = false; vm.leave(onBack) }, onDismiss = { confirmLeave = false },
    )
}
