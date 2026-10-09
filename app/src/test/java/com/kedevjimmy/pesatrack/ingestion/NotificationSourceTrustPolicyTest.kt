package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSourceTrustPolicyTest {
    private val self = "com.kedevjimmy.pesatrack"

    @Test fun unknownForwarderCannotUseApprovedBanksIdentity() {
        assertFalse(NotificationSourceTrustPolicy.isApproved("ke.example.forwarder", self, setOf("ke.example.bank")))
    }
    @Test fun approvalAndRevocationUseExactPackage() {
        assertTrue(NotificationSourceTrustPolicy.isApproved("ke.example.bank", self, setOf("ke.example.bank")))
        assertFalse(NotificationSourceTrustPolicy.isApproved("ke.example.bank", self, emptySet()))
        assertFalse(NotificationSourceTrustPolicy.isApproved("KE.EXAMPLE.BANK", self, setOf("ke.example.bank")))
    }
    @Test fun selfAndMissingSourcesAreRejectedEvenIfListed() {
        val approved = setOf(self, "")
        assertFalse(NotificationSourceTrustPolicy.isApproved(self, self, approved))
        assertFalse(NotificationSourceTrustPolicy.isApproved("", self, approved))
        assertFalse(NotificationSourceTrustPolicy.isApproved(null, self, approved))
    }
}
