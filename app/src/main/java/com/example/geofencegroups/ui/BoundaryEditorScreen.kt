package com.example.geofencegroups.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geofencegroups.AppContainer
import com.example.geofencegroups.data.model.GeofenceConfig
import com.example.geofencegroups.permissions.Perms
import com.example.geofencegroups.ui.components.*
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@Composable
fun BoundaryEditorScreen(c: AppContainer, groupId: String, onDone: () -> Unit) {
    val vm: GroupDetailViewModel = viewModel(factory = vmFactory { GroupDetailViewModel(c, groupId) })
    val group by vm.group.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    OrbitScreen("Set boundary", onBack = onDone) {
        val g = group
        if (g == null) LoadingBlock()
        else EditorContent(g.geofence, busy, message) { lat, lng, r -> vm.saveGeofence(lat, lng, r, onDone) }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun EditorContent(initial: GeofenceConfig?, busy: Boolean, message: String?, onSave: (Double, Double, Double) -> Unit) {
    val ctx = LocalContext.current
    var latT by remember { mutableStateOf(initial?.lat?.toString() ?: "") }
    var lngT by remember { mutableStateOf(initial?.lng?.toString() ?: "") }
    var radius by remember { mutableFloatStateOf((initial?.radiusM ?: 300.0).toFloat().coerceIn(100f, 5000f)) }
    var manual by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val center = remember(latT, lngT) {
        val a = latT.toDoubleOrNull(); val b = lngT.toDoubleOrNull()
        if (a != null && b != null && a in -90.0..90.0 && b in -180.0..180.0) LatLng(a, b) else null
    }

    fun setCenter(lat: Double, lng: Double) {
        latT = String.format(Locale.US, "%.6f", lat); lngT = String.format(Locale.US, "%.6f", lng)
    }
    fun useCurrent() {
        LocationServices.getFusedLocationProviderClient(ctx).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) Toast.makeText(ctx, "Couldn't get your location. Is location turned on?", Toast.LENGTH_SHORT).show()
                else setCenter(loc.latitude, loc.longitude)
            }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Perms.fine(ctx)) useCurrent()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = Space.lg).padding(bottom = Space.lg + navBarPadding()).imePadding(),
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Text(if (center == null) "Tap the map to place the centre." else "Tap to move the centre. Members are watched inside this circle.",
            style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft)
        BoundaryMap(center, radius.toDouble(), Modifier.weight(1f).fillMaxWidth(), interactive = true,
            onTap = { setCenter(it.latitude, it.longitude) }, cameraKey = "editor")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            SecondaryButton("Use my location", {
                if (Perms.fine(ctx)) useCurrent()
                else permLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.weight(1f))
            SecondaryButton(if (manual) "Hide coordinates" else "Enter coordinates", { manual = !manual }, Modifier.weight(1f))
        }
        if (manual) Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OrbitField(latT, { latT = it }, "Latitude", Modifier.weight(1f), KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OrbitField(lngT, { lngT = it }, "Longitude", Modifier.weight(1f), KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        Text("Radius · ${radius.toInt()} m", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(150f, 300f, 500f, 1000f).forEach { r ->
                val on = radius == r
                Box(Modifier.weight(1f)) {
                    if (on) AccentButton("${r.toInt()} m", { radius = r }, Modifier.fillMaxWidth())
                    else SecondaryButton("${r.toInt()} m", { radius = r }, Modifier.fillMaxWidth())
                }
            }
        }
        Slider(radius, { radius = it }, valueRange = 100f..5000f,
            colors = SliderDefaults.colors(thumbColor = Orbit.Forest, activeTrackColor = Orbit.Moss, inactiveTrackColor = Orbit.Sand))
        Text("Under about 150 m, Android may detect crossings late or not at all.", style = MaterialTheme.typography.bodySmall, color = Orbit.ForestSoft)
        message?.let { ErrorBanner(it) }
        PrimaryButton("Save boundary", modifier = Modifier.fillMaxWidth(), enabled = center != null, loading = busy, onClick = {
            if (initial != null) confirm = true else center?.let { onSave(it.latitude, it.longitude, radius.toDouble()) }
        })
    }

    if (confirm) ConfirmDialog(
        title = "Change the boundary?", confirmLabel = "Change it",
        body = "Everyone's status resets to “not reported” until their phone checks in against the new boundary.",
        onConfirm = { confirm = false; center?.let { onSave(it.latitude, it.longitude, radius.toDouble()) } },
        onDismiss = { confirm = false },
    )
}
