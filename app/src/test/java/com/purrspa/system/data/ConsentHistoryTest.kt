package com.purrspa.system.data

import org.junit.Assert.*
import org.junit.Test

class ConsentHistoryTest {
    private fun event(id: String, catId: String, time: Long) =
        ConsentEvent(id, catId, false, false, time)

    @Test fun onlyReturnsEventsForRequestedCatNewestFirst() {
        val events = listOf(
            event("old", "a", 100),
            event("other", "b", 300),
            event("new", "a", 200)
        )
        assertEquals(listOf("new", "old"), ConsentHistory.forCat(events, "a").map { it.id })
    }

    @Test fun latestReturnsNullForCatWithoutHistory() {
        assertNull(ConsentHistory.latest(listOf(event("a", "other", 1)), "missing"))
    }

    @Test fun equalTimestampsHaveDeterministicOrder() {
        val events = listOf(event("a", "cat", 100), event("b", "cat", 100))
        assertEquals(listOf("b", "a"), ConsentHistory.forCat(events, "cat").map { it.id })
    }
}
