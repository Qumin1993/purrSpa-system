package com.purrspa.system.data

import org.junit.Assert.*
import org.junit.Test

class VisitBalanceTest {
    private fun visit(
        price: Long = 4000,
        travel: Long = 500,
        deposit: Long = 1000,
        payment: String = "UNPAID"
    ) = Visit(
        id = "visit", catId = "cat", startMillis = 1L,
        service = "Groom", location = "Salon", pricePence = price,
        travelFeePence = travel, depositPaidPence = deposit, paymentStatus = payment
    )

    @Test fun outstandingSubtractsDepositAndIncludesTravel() {
        assertEquals(3500L, VisitBalance.outstandingPence(visit()))
    }

    @Test fun fullyPaidHasNoOutstandingBalance() {
        assertEquals(0L, VisitBalance.outstandingPence(visit(payment = "PAID")))
    }

    @Test fun rejectsInvalidDepositsAndNegativeFees() {
        assertFalse(VisitBalance.validCharges(4000, 500, 4501))
        assertFalse(VisitBalance.validCharges(4000, -1, 0))
        assertTrue(VisitBalance.validCharges(4000, 500, 4500))
    }

    @Test fun detectsOverflow() {
        assertNull(VisitBalance.totalPence(visit(price = Long.MAX_VALUE, travel = 1)))
        assertFalse(VisitBalance.validCharges(Long.MAX_VALUE, 1, 0))
    }

    @Test fun cancelledAndNoShowCannotBeMarkedPaid() {
        assertFalse(VisitBalance.paymentStatusAllowed("CANCELLED", "PAID"))
        assertFalse(VisitBalance.paymentStatusAllowed("NO_SHOW", "PAID"))
        assertTrue(VisitBalance.paymentStatusAllowed("COMPLETED", "PAID"))
        assertTrue(VisitBalance.paymentStatusAllowed("CANCELLED", "UNPAID"))
    }
}
