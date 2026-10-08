package com.purrspa.system.data

object VisitMoney {
    fun totalPence(servicePence: Long, travelFeePence: Long): Long {
        require(servicePence >= 0 && travelFeePence >= 0)
        return Math.addExact(servicePence, travelFeePence)
    }

    fun validDeposit(servicePence: Long, travelFeePence: Long, depositPaidPence: Long): Boolean =
        runCatching {
            depositPaidPence >= 0 && depositPaidPence <= totalPence(servicePence, travelFeePence)
        }.getOrDefault(false)

    fun outstandingPence(servicePence: Long, travelFeePence: Long, depositPaidPence: Long, paymentStatus: String): Long {
        require(validDeposit(servicePence, travelFeePence, depositPaidPence))
        require(paymentStatus == "UNPAID" || paymentStatus == "PAID")
        return if (paymentStatus == "PAID") 0L else totalPence(servicePence, travelFeePence) - depositPaidPence
    }
}
