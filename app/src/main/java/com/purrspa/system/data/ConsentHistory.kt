package com.purrspa.system.data

/** Selects a cat's consent decisions, newest first. */
object ConsentHistory {
    fun forCat(events: List<ConsentEvent>, catId: String): List<ConsentEvent> =
        events.filter { it.catId == catId }
            .sortedWith(compareByDescending<ConsentEvent> { it.recordedMillis }.thenByDescending { it.id })

    fun latest(events: List<ConsentEvent>, catId: String): ConsentEvent? =
        forCat(events, catId).firstOrNull()
}
