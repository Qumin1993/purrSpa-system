package com.purrspa.system.data

/** Amounts are integer pence. A PAID flag records a manual full-payment confirmation. */
object VisitBalance {
    fun totalPence(visit: Visit): Long? =
        runCatching { Math.addExact(visit.pricePence, visit.travelFeePence) }.getOrNull()

    fun outstandingPence(visit: Visit): Long? {
        if (visit.paymentStatus == "PAID") return 0L
        val total = totalPence(visit) ?: return null
        return runCatching { Math.subtractExact(total, visit.depositPaidPence) }
            .getOrNull()?.coerceAtLeast(0L)
    }

    fun paymentStatusAllowed(visitStatus: String, nextPaymentStatus: String): Boolean =
        nextPaymentStatus == "UNPAID" ||
            (nextPaymentStatus == "PAID" && visitStatus !in setOf("CANCELLED", "NO_SHOW"))

    fun validCharges(servicePence: Long, travelPence: Long, depositPence: Long): Boolean {
        if (servicePence < 0 || travelPence < 0 || depositPence < 0) return false
        val total = runCatching { Math.addExact(servicePence, travelPence) }.getOrNull() ?: return false
        return depositPence <= total
    }
}
