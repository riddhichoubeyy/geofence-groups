package com.example.geofencegroups.data.model

import com.example.geofencegroups.domain.Pulse
import com.google.firebase.firestore.DocumentSnapshot

data class GeofenceConfig(val lat: Double, val lng: Double, val radiusM: Double)

data class Group(
    val id: String, val name: String, val ownerUid: String,
    val memberUids: List<String>, val geofence: GeofenceConfig?, val pulse: Pulse,
)

data class Member(
    val uid: String, val displayName: String, val role: String,
    val insideGeofence: Boolean?, val lastTransitionMs: Long?,
)

data class GeoEvent(val id: String, val uid: String, val displayName: String, val type: String, val timeMs: Long?)

private fun DocumentSnapshot.millis(field: String): Long? = getTimestamp(field)?.toDate()?.time

fun DocumentSnapshot.toGroup(): Group? {
    if (!exists()) return null
    val fence = (get("geofence") as? Map<*, *>)?.let { m ->
        val lat = (m["lat"] as? Number)?.toDouble()
        val lng = (m["lng"] as? Number)?.toDouble()
        val r = (m["radiusM"] as? Number)?.toDouble()
        if (lat != null && lng != null && r != null) GeofenceConfig(lat, lng, r) else null
    }
    val p = get("pulse") as? Map<*, *>
    val pulse = Pulse(
        inside = (p?.get("inside") as? Number)?.toInt() ?: 0,
        outside = (p?.get("outside") as? Number)?.toInt() ?: 0,
        unknown = (p?.get("unknown") as? Number)?.toInt() ?: 0,
        lastEventName = p?.get("lastEventName") as? String,
        lastEventType = p?.get("lastEventType") as? String,
        lastEventAtMs = (p?.get("lastEventAt") as? com.google.firebase.Timestamp)?.toDate()?.time,
    )
    return Group(
        id = id, name = getString("name") ?: "", ownerUid = getString("ownerUid") ?: "",
        memberUids = (get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        geofence = fence, pulse = pulse,
    )
}

fun DocumentSnapshot.toMember() = Member(
    uid = id, displayName = getString("displayName") ?: "Member",
    role = getString("role") ?: "member", insideGeofence = getBoolean("insideGeofence"),
    lastTransitionMs = millis("lastTransitionAt"),
)

fun DocumentSnapshot.toEvent() = GeoEvent(
    id = id, uid = getString("uid") ?: "", displayName = getString("displayName") ?: "Member",
    type = getString("type") ?: "", timeMs = millis("serverTime"),
)

fun Throwable.userMessage(): String = when (this) {
    is com.google.firebase.FirebaseNetworkException -> "No connection. Check your network and try again."
    is com.google.firebase.functions.FirebaseFunctionsException ->
        if (code == com.google.firebase.functions.FirebaseFunctionsException.Code.UNAVAILABLE)
            "Can't reach Orbit right now. Try again in a moment."
        else message ?: "Something went wrong."
    else -> message ?: "Something went wrong."
}
