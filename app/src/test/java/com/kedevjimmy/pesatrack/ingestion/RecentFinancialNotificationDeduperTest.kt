package com.kedevjimmy.pesatrack.ingestion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentFinancialNotificationDeduperTest {
    @Test
    fun identicalNotificationUpdateIsSuppressedInsideWindow() {
        val deduper =
            RecentFinancialNotificationDeduper(
                windowMs = 120_000L
            )

        assertTrue(
            deduper.shouldProcess(
                notificationKey = "bank|1",
                sourcePackage =
                    "ke.example.bank",
                title =
                    "Transaction alert",
                body =
                    "KES 500.00 debited from account.",
                observedAtEpochMs =
                    1_000L
            )
        )
        assertFalse(
            deduper.shouldProcess(
                notificationKey = "bank|1",
                sourcePackage =
                    "ke.example.bank",
                title =
                    "Transaction alert",
                body =
                    "KES 500.00 debited from account.",
                observedAtEpochMs =
                    30_000L
            )
        )
    }

    @Test
    fun changedNotificationBodyIsNewEvidence() {
        val deduper =
            RecentFinancialNotificationDeduper(
                windowMs = 120_000L
            )

        deduper.shouldProcess(
            notificationKey = "bank|1",
            sourcePackage =
                "ke.example.bank",
            title =
                "Transaction alert",
            body =
                "KES 500.00 debited from account.",
            observedAtEpochMs =
                1_000L
        )

        assertTrue(
            deduper.shouldProcess(
                notificationKey = "bank|1",
                sourcePackage =
                    "ke.example.bank",
                title =
                    "Transaction alert",
                body =
                    "KES 700.00 debited from account.",
                observedAtEpochMs =
                    10_000L
            )
        )
    }

    @Test
    fun sameNotificationCanBeProcessedAgainAfterWindow() {
        val deduper =
            RecentFinancialNotificationDeduper(
                windowMs = 120_000L
            )

        deduper.shouldProcess(
            notificationKey = "bank|1",
            sourcePackage =
                "ke.example.bank",
            title =
                "Transaction alert",
            body =
                "KES 500.00 debited from account.",
            observedAtEpochMs =
                1_000L
        )

        assertTrue(
            deduper.shouldProcess(
                notificationKey = "bank|1",
                sourcePackage =
                    "ke.example.bank",
                title =
                    "Transaction alert",
                body =
                    "KES 500.00 debited from account.",
                observedAtEpochMs =
                    122_000L
            )
        )
    }

    @Test
    fun identicalTextFromDifferentNotificationKeysIsNotCollapsed() {
        val deduper =
            RecentFinancialNotificationDeduper(
                windowMs = 120_000L
            )

        deduper.shouldProcess(
            notificationKey = "bank|1",
            sourcePackage =
                "ke.example.bank",
            title =
                "Transaction alert",
            body =
                "KES 500.00 debited from account.",
            observedAtEpochMs =
                1_000L
        )

        assertTrue(
            deduper.shouldProcess(
                notificationKey = "bank|2",
                sourcePackage =
                    "ke.example.bank",
                title =
                    "Transaction alert",
                body =
                    "KES 500.00 debited from account.",
                observedAtEpochMs =
                    2_000L
            )
        )
    }
}
