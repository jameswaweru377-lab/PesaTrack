package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.BundledKenyaMessagePackLoader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialSmsSenderPolicyTest {
    @Test fun recognisesFinancialInstitutionSenderWithoutReadingBody() {
        listOf("M-PESA", "Safaricom", "EQUITY", "Co-op Bank", "I&M", "TALA", "Branch", "M-Shwari",
            "HUSTLER FND", "DTBK", "PESALINK", "GLOBALPAY", "EVOPAY", "ETICA", "SANLAM_UT")
            .forEach { sender ->
                assertTrue("Expected recognised financial sender $sender", FinancialSmsSenderPolicy.isTrusted(sender))
            }
    }

    @Test fun doesNotTrustPersonalNumbersOrPartialBrandMatches() {
        listOf(null, "", "0712340000", "+254712340000", "254712340000", "EQUITY-HELP", "My Equity Friend", "BRANCHING", "TALAPROMO", "12345")
            .forEach { sender ->
                assertFalse("Unexpected trusted sender $sender", FinancialSmsSenderPolicy.isTrusted(sender))
            }
    }

    @Test fun bankSenderSuffixesRemainBounded() {
        assertTrue(FinancialSmsSenderPolicy.isTrusted("EQUITYBANKSMS"))
        assertFalse(FinancialSmsSenderPolicy.isTrusted("EQUITYBANKPROMO"))
        assertFalse(FinancialSmsSenderPolicy.isTrusted("KCB-HELP"))
    }

    @Test fun explicitSenderApprovalIsExactAndCannotApproveAnEmptyValue() {
        val approvals = setOf("ZENFIN", "12345")
        assertTrue(FinancialSmsSenderPolicy.isTrusted("ZENFIN", approvals))
        assertTrue(FinancialSmsSenderPolicy.isTrusted("12345", approvals))
        assertFalse(FinancialSmsSenderPolicy.isTrusted("ZENFIN-HELP", approvals))
        assertFalse(FinancialSmsSenderPolicy.isTrusted("0712340000", setOf("")))
    }

    @Test fun rejectsPhoneNumbersEvenWhenTypedIntoCustomApproval() {
        assertFalse(FinancialSmsSenderPolicy.canApprove("0712340000"))
        assertFalse(FinancialSmsSenderPolicy.canApprove("+254712340000"))
        assertFalse(FinancialSmsSenderPolicy.canApprove("12"))
        assertTrue(FinancialSmsSenderPolicy.canApprove("12345"))
        assertTrue(FinancialSmsSenderPolicy.canApprove("ZenFin"))
    }

    @Test fun unicodeAndConfusableSenderAddressesFailClosed() {
        listOf("EQUITYé", "ЕQUITY", "EQUITY\u0661", "MPESA\u200D", "071234\u0660\u0660\u0660\u0660")
            .forEach { sender ->
                assertFalse("Unexpected trusted sender $sender", FinancialSmsSenderPolicy.isTrusted(sender))
                assertFalse("Unexpected approvable sender $sender", FinancialSmsSenderPolicy.canApprove(sender))
            }
        assertFalse(FinancialSmsSenderPolicy.isTrusted("ZENFIN\u200D", setOf("ZENFIN")))
    }

    @Test fun everyShippedKenyanFinancialMessageSenderPassesThePrivateGate() {
        val senders = BundledKenyaMessagePackLoader.load().templates
            .flatMap { it.senderIds }
            .distinct()
        assertTrue("No sender IDs found in bundled message packs", senders.isNotEmpty())
        senders.forEach { sender ->
            assertTrue("Shipped message pack sender would be skipped: $sender",
                FinancialSmsSenderPolicy.isTrusted(sender))
        }
    }
}
