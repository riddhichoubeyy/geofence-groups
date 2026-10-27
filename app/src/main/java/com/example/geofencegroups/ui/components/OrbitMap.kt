package com.example.geofencegroups.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.geofencegroups.ui.theme.Orbit
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlin.math.ln

/** Muted map style: warm land, soft green parks, quiet roads, no POI clutter. */
private const val ORBIT_MAP_STYLE = """
[
 {"elementType":"geometry","stylers":[{"color":"#f2eddf"}]},
 {"elementType":"labels.text.fill","stylers":[{"color":"#55695e"}]},
 {"elementType":"labels.text.stroke","stylers":[{"color":"#f7f3ea"}]},
 {"featureType":"poi","stylers":[{"visibility":"off"}]},
 {"featureType":"transit","stylers":[{"visibility":"off"}]},
 {"featureType":"poi.park","stylers":[{"visibility":"on"}]},
 {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#dbe8c0"}]},
 {"featureType":"road","elementType":"geometry","stylers":[{"color":"#fffcf5"}]},
 {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#e6dcc0"}]},
 {"featureType":"water","elementType":"geometry","stylers":[{"color":"#cfe0d8"}]}
]
"""

fun zoomFor(radiusM: Double): Float = (15.0 - ln(radiusM / 500.0) / ln(2.0)).toFloat().coerceIn(6f, 17f)

/** The boundary: forest outline, soft lime fill, and a centre marker. */
@Composable
fun BoundaryMap(
    center: LatLng?, radiusM: Double, modifier: Modifier = Modifier, interactive: Boolean = false,
    onTap: ((LatLng) -> Unit)? = null, cameraKey: Any? = null,
) {
    val camera = rememberCameraPositionState(key = cameraKey?.toString()) {
        position = CameraPosition.fromLatLngZoom(center ?: LatLng(20.5937, 78.9629), if (center != null) zoomFor(radiusM) else 4f)
    }
    androidx.compose.runtime.LaunchedEffect(center) {
        center?.let { camera.animate(CameraUpdateFactory.newLatLng(it)) }
    }
    val style = remember { MapStyleOptions(ORBIT_MAP_STYLE) }
    Box(modifier.clip(androidx.compose.material3.MaterialTheme.shapes.large)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
            properties = MapProperties(mapStyleOptions = style),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false,
                scrollGesturesEnabled = interactive, zoomGesturesEnabled = interactive,
                rotationGesturesEnabled = false, tiltGesturesEnabled = false,
            ),
            onMapClick = { ll -> onTap?.invoke(ll) },
        ) {
            if (center != null) {
                Circle(center = center, radius = radiusM, strokeColor = Orbit.Forest,
                    fillColor = Orbit.Lime.copy(alpha = 0.28f), strokeWidth = 5f)
                Marker(state = MarkerState(position = center),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
        }
    }
}
