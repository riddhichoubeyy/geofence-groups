package com.example.geofencegroups.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.permissions.Perms
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space

@Composable
fun SettingsScreen(c: AppContainer, hasBoundary: Boolean, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = vmFactory { SettingsViewModel(c) })
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    var confirmSignOut by remember { mutableStateOf(false) }
    val monitoring = rememberMonitoring(c, hasBoundary)

    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val fine = remember(tick) { Perms.fine(ctx) }
    val bg = remember(tick) { Perms.background(ctx) }
    val notif = remember(tick) { Perms.notifications(ctx) }
    val battery = remember(tick) { Perms.batteryUnrestricted(ctx) }

    fun openAppSettings() = ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    )
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }

    OrbitScreen("Settings", onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg)
            .padding(bottom = Space.xxl + navBarPadding()), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            MonitoringBanner(monitoring, onFix = null)
            SectionTitle("Permissions")
            PermissionRow("Location while using Orbit", "Lets you place the boundary at your current spot.",
                fine, "Allow", true) { multi.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            PermissionRow("Location “Allow all the time”",
                "Android only reports boundary crossings to a closed app with this setting. On Android 11 and newer, choose it on the page that opens.",
                bg, if (Build.VERSION.SDK_INT >= 30) "Open settings" else "Allow", fine) {
                if (Build.VERSION.SDK_INT >= 30) openAppSettings() else single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            PermissionRow("Notifications", "So you hear when someone leaves or comes back.", notif, "Allow", true) {
                if (Build.VERSION.SDK_INT >= 33) single.launch(Manifest.permission.POST_NOTIFICATIONS) else openAppSettings()
            }
            PermissionRow("Battery: unrestricted", "Some phones pause background apps. Setting Orbit to unrestricted makes alerts more reliable.",
                battery, "Open settings", true) { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }

            SectionTitle("Privacy")
            OrbitCard {
                Text("What Orbit keeps", style = MaterialTheme.typography.titleSmall)
                Text("Your name and email, the groups you're in, each group's boundary, and a log of who left or came back and when.",
                    style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
                Spacer(Modifier.height(Space.md))
                Text("What it doesn't", style = MaterialTheme.typography.titleSmall)
                Text("Your position stays on your phone. A crossing sends one coordinate to the server only to check it's plausible; it isn't stored, and nobody can see where you are or where you've been.",
                    style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
                Spacer(Modifier.height(Space.md))
                Text("Who sees what", style = MaterialTheme.typography.titleSmall)
                Text("Group members see the boundary, each other's names, inside/outside status and the activity log.",
                    style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
            }

            SectionTitle("Account")
            Text(c.firebaseAuth.currentUser?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            SecondaryButton("Sign out", { confirmSignOut = true }, Modifier.fillMaxWidth())
            if (version.isNotEmpty()) Text("Orbit $version", style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
        }
    }

    if (confirmSignOut) ConfirmDialog(
        title = "Sign out of Orbit?", confirmLabel = "Sign out", destructive = true,
        body = "This phone will stop watching your boundaries and stop receiving alerts until you sign in again.",
        onConfirm = { confirmSignOut = false; vm.signOut() }, onDismiss = { confirmSignOut = false },
    )
}

@Composable
private fun PermissionRow(title: String, desc: String, done: Boolean, action: String, enabled: Boolean, onClick: () -> Unit) {
    OrbitCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusPill(if (done) "On" else "Needed", if (done) Tone.Good else Tone.Warn)
        }
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
        if (!done) { Spacer(Modifier.height(Space.sm)); SecondaryButton(action, onClick, enabled = enabled) }
    }
}
