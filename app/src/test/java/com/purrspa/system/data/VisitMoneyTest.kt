package com.purrspa.system.data

import org.junit.Test
import org.junit.Assert.assertEquals

class VisitMoneyTest {
    @Test fun totalIncludesTravel() {
        assertEquals(6500L, VisitMoney.totalPence(4500L, 2000L))
    }
}
