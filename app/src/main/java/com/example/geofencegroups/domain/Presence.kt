package com.example.geofencegroups.domain

enum class PresenceState { INSIDE, OUTSIDE, UNKNOWN }

/**
 * A member's status is only updated when their device crosses the boundary, so it is "last known",
 * never live. [stale] is true when the last crossing is old enough that it may no longer be true.
 */
data class Presence(val state: PresenceState, val sinceMs: Long?, val stale: Boolean)

const val PRESENCE_STALE_AFTER_MS = 48L * 60 * 60 * 1000

fun presenceOf(inside: Boolean?, lastTransitionMs: Long?, nowMs: Long): Presence {
    val state = when (inside) { true -> PresenceState.INSIDE; false -> PresenceState.OUTSIDE; null -> PresenceState.UNKNOWN }
    if (state == PresenceState.UNKNOWN) return Presence(state, null, false)
    val stale = lastTransitionMs == null || nowMs - lastTransitionMs > PRESENCE_STALE_AFTER_MS
    return Presence(state, lastTransitionMs, stale)
}

/** Human label that never implies live data: stale and old values are labelled "last known". */
fun presenceLabel(p: Presence, nowMs: Long): String {
    val word = when (p.state) {
        PresenceState.INSIDE -> "inside"
        PresenceState.OUTSIDE -> "outside"
        PresenceState.UNKNOWN -> return "Hasn't reported yet"
    }
    val ago = p.sinceMs?.let { relativeTime(it, nowMs) }
    return when {
        p.stale && ago != null -> "Last known $word · $ago"
        p.stale -> "Last known $word"
        ago != null -> "${word.replaceFirstChar { it.uppercase() }} · since $ago"
        else -> word.replaceFirstChar { it.uppercase() }
    }
}
