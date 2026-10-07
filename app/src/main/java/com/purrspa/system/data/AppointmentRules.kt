package com.purrspa.system.data

/**
 * Prototype scheduling rules. Visits currently have no stored duration, so a configurable
 * default duration is used until service durations are modeled explicitly.
 */
object AppointmentRules {
    const val DEFAULT_DURATION_MINUTES = 120L
    private const val MINUTE_MILLIS = 60_000L

    fun overlaps(
        firstStart: Long,
        firstDurationMinutes: Long,
        secondStart: Long,
        secondDurationMinutes: Long
    ): Boolean {
        require(firstDurationMinutes > 0 && secondDurationMinutes > 0)
        val firstEnd = Math.addExact(firstStart, Math.multiplyExact(firstDurationMinutes, MINUTE_MILLIS))
        val secondEnd = Math.addExact(secondStart, Math.multiplyExact(secondDurationMinutes, MINUTE_MILLIS))
        return firstStart < secondEnd && secondStart < firstEnd
    }

    fun conflicts(startMillis: Long, existing: List<Visit>): Boolean =
        existing.any { visit ->
            visit.status != "CANCELLED" &&
                visit.status != "NO_SHOW" &&
                overlaps(startMillis, DEFAULT_DURATION_MINUTES, visit.startMillis, DEFAULT_DURATION_MINUTES)
        }
}
