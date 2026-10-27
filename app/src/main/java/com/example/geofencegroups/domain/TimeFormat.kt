package com.example.geofencegroups.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun relativeTime(thenMs: Long, nowMs: Long): String {
    val diff = nowMs - thenMs
    return when {
        diff < 0 || diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} h ago"
        diff < 2 * 86_400_000L -> "yesterday"
        else -> "${diff / 86_400_000L} days ago"
    }
}

private fun dayStart(ms: Long, tz: TimeZone): Long = Calendar.getInstance(tz).apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Today", "Yesterday", or "Oct 24" — used to group timeline entries. */
fun dayLabel(thenMs: Long, nowMs: Long, tz: TimeZone = TimeZone.getDefault()): String {
    val days = Math.round((dayStart(nowMs, tz) - dayStart(thenMs, tz)) / 86_400_000.0)
    if (days <= 0) return "Today"
    if (days == 1L) return "Yesterday"
    val c = Calendar.getInstance(tz, Locale.US).apply { timeInMillis = thenMs }
    return "${MONTHS[c.get(Calendar.MONTH)]} ${c.get(Calendar.DAY_OF_MONTH)}"
}

fun clockTime(ms: Long, tz: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(tz, Locale.US).apply { timeInMillis = ms }
    return "%02d:%02d".format(Locale.US, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}
