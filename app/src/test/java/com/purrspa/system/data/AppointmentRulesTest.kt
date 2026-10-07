package com.purrspa.system.data

import org.junit.Assert.*
import org.junit.Test

class AppointmentRulesTest {
    @Test fun overlappingIntervalsConflict() {
        assertTrue(AppointmentRules.overlaps(1_000_000L, 120, 1_000_000L + 60 * 60_000L, 120))
    }

    @Test fun touchingIntervalsDoNotConflict() {
        assertFalse(AppointmentRules.overlaps(1_000_000L, 120, 1_000_000L + 120 * 60_000L, 120))
    }

    @Test fun cancelledVisitDoesNotBlockTime() {
        val cancelled = Visit("v1", "cat", 1_000_000L, "Bath", "Salon", 4000, "CANCELLED")
        assertFalse(AppointmentRules.conflicts(1_000_000L, listOf(cancelled)))
    }

    @Test fun activeVisitBlocksTime() {
        val scheduled = Visit("v1", "cat", 1_000_000L, "Bath", "Salon", 4000)
        assertTrue(AppointmentRules.conflicts(1_000_000L, listOf(scheduled)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationIsRejected() {
        AppointmentRules.overlaps(0, 0, 0, 60)
    }
}
