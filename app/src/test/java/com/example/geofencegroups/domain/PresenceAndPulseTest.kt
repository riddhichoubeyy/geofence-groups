package com.example.geofencegroups.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceAndPulseTest {
    private val now = 10_000_000_000L

    @Test fun unknownHasNoTimeAndIsNeverStale() {
        val p = presenceOf(null, null, now)
        assertEquals(PresenceState.UNKNOWN, p.state); assertNull(p.sinceMs); assertFalse(p.stale)
    }

    @Test fun recentInsideIsFresh() {
        val p = presenceOf(true, now - 60_000, now)
        assertEquals(PresenceState.INSIDE, p.state); assertFalse(p.stale)
    }

    @Test fun oldStatusIsMarkedStaleNotLive() {
        val p = presenceOf(false, now - PRESENCE_STALE_AFTER_MS - 1, now)
        assertEquals(PresenceState.OUTSIDE, p.state); assertTrue(p.stale)
    }

    @Test fun knownStateWithoutTimestampIsStale() = assertTrue(presenceOf(true, null, now).stale)

    @Test fun noBoundaryHeadline() =
        assertEquals(PulseLevel.NO_BOUNDARY, pulseHeadline(Pulse(0, 0, 2), hasBoundary = false).level)

    @Test fun waitingWhenNobodyReported() {
        val h = pulseHeadline(Pulse(0, 0, 3), true)
        assertEquals(PulseLevel.WAITING, h.level); assertEquals("3 members haven't reported yet.", h.detail)
    }

    @Test fun someoneOutsideIsSingularAndMentionsUnreported() {
        val h = pulseHeadline(Pulse(2, 1, 1), true)
        assertEquals(PulseLevel.SOMEONE_OUTSIDE, h.level)
        assertEquals("1 member is outside", h.title); assertEquals("2 inside · 1 not reported", h.detail)
    }

    @Test fun everyoneInsideOnlyWhenAllReported() {
        assertEquals("Everyone is inside", pulseHeadline(Pulse(3, 0, 0), true).title)
        assertEquals("2 inside, none outside", pulseHeadline(Pulse(2, 0, 1), true).title)
    }

    @Test fun relativeTimeBuckets() {
        assertEquals("just now", relativeTime(now - 5_000, now))
        assertEquals("5 min ago", relativeTime(now - 5 * 60_000, now))
        assertEquals("3 h ago", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("yesterday", relativeTime(now - 30 * 3_600_000, now))
        assertEquals("4 days ago", relativeTime(now - 4 * 86_400_000L, now))
    }

    @Test fun dayLabelsGroupByCalendarDay() {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val base = 1_700_000_000_000L // 2023-11-14 22:13:20 UTC
        assertEquals("Today", dayLabel(base - 3_600_000, base, utc))
        assertEquals("Yesterday", dayLabel(base - 86_400_000, base, utc))
        assertEquals("Nov 10", dayLabel(base - 4 * 86_400_000L, base, utc))
    }

    @Test fun labelsNeverImplyLiveWhenStale() {
        assertEquals("Inside · since 5 min ago", presenceLabel(presenceOf(true, now - 5 * 60_000, now), now))
        assertEquals("Last known outside · 4 days ago", presenceLabel(presenceOf(false, now - 4 * 86_400_000L, now), now))
        assertEquals("Hasn't reported yet", presenceLabel(presenceOf(null, null, now), now))
    }
}
