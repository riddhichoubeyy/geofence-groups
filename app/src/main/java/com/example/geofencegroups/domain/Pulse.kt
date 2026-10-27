package com.example.geofencegroups.domain

/** Counts maintained by the backend on each crossing. Contains no coordinates. */
data class Pulse(
    val inside: Int = 0,
    val outside: Int = 0,
    val unknown: Int = 0,
    val lastEventName: String? = null,
    val lastEventType: String? = null,
    val lastEventAtMs: Long? = null,
)

enum class PulseLevel { NO_BOUNDARY, WAITING, ALL_INSIDE, SOMEONE_OUTSIDE }

data class PulseHeadline(val title: String, val detail: String, val level: PulseLevel)

private fun members(n: Int) = if (n == 1) "1 member" else "$n members"

fun pulseHeadline(p: Pulse, hasBoundary: Boolean): PulseHeadline {
    if (!hasBoundary) return PulseHeadline("No boundary yet", "Set one to start watching.", PulseLevel.NO_BOUNDARY)
    val known = p.inside + p.outside
    return when {
        known == 0 -> PulseHeadline(
            "Waiting for first reports",
            if (p.unknown == 1) "1 member hasn't reported yet." else "${p.unknown} members haven't reported yet.",
            PulseLevel.WAITING,
        )
        p.outside > 0 -> PulseHeadline(
            if (p.outside == 1) "1 member is outside" else "${p.outside} members are outside",
            buildString {
                append("${p.inside} inside")
                if (p.unknown > 0) append(" · ${p.unknown} not reported")
            },
            PulseLevel.SOMEONE_OUTSIDE,
        )
        p.unknown > 0 -> PulseHeadline(
            "${p.inside} inside, none outside", "${members(p.unknown)} yet to report.", PulseLevel.ALL_INSIDE,
        )
        else -> PulseHeadline("Everyone is inside", "All ${members(p.inside)} reported inside.", PulseLevel.ALL_INSIDE)
    }
}
