package com.kedevjimmy.pesatrack.domain.planning

import com.kedevjimmy.pesatrack.planning.BillCadence as ScheduleBillCadence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BillCadenceDomainContractTest {
    @Test
    fun savedCadenceNamesRemainCompatibleWithSchedulingApi() {
        val savedNames = listOf("MONTHLY", "WEEKLY")

        assertEquals(savedNames, BillCadence.entries.map { it.name })
        savedNames.forEach { savedName ->
            assertSame(
                BillCadence.valueOf(savedName),
                ScheduleBillCadence.valueOf(savedName)
            )
        }
    }
}
