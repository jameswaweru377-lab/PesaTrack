package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FingerprintConflictStoreTest {
    private lateinit var context: Context
    private val epoch = 1_790_000_000_000L

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    private fun observation(amount: Long, fee: Long = 0L) = ParsedMpesaTransaction(
        transactionCode = "SYNTHETIC123", type = MpesaTransactionType.SENT_MONEY,
        amountMinor = amount, counterparty = "Invented Vendor", feeMinor = fee,
        balanceMinor = null, occurredAtText = null, rawMessage = "Invented test only"
    )

    private fun count(table: String): Int = PesaTrackDatabase.get(context).readableDatabase
        .rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    @Test fun conflictingAmountPreservesOriginalAndCreatesOneNonPostingReviewRow() {
        val store = TransactionStore.get(context)
        assertTrue(store.insertIfNew(observation(45_000L), null, "sms", epoch))
        val before = store.getByTransactionCode("SYNTHETIC123")!!
        assertFalse(store.insertIfNew(observation(46_000L), null, "notification", epoch + 1000L))
        assertFalse(store.insertIfNew(observation(46_000L), null, "notification", epoch + 1000L))
        assertEquals(1, count("transactions"))
        assertEquals(1, count("fingerprint_conflicts"))
        val after = store.getByTransactionCode("SYNTHETIC123")!!
        assertEquals(before, after)
    }

    @Test fun identicalReplayDoesNotCreateReviewRow() {
        val store = TransactionStore.get(context)
        store.insertIfNew(observation(45_000L), null, "sms", epoch)
        assertFalse(store.insertIfNew(observation(45_000L), null, "notification", epoch + 1000L))
        assertEquals(0, count("fingerprint_conflicts"))
        assertEquals(1, count("transactions"))
    }

    @Test fun conflictingFeeCannotChangeCanonicalTransaction() {
        val store = TransactionStore.get(context)
        store.insertIfNew(observation(45_000L), null, "sms", epoch)
        assertFalse(store.insertIfNew(observation(45_000L, 200L), null, "sms", epoch))
        assertEquals(0L, store.getByTransactionCode("SYNTHETIC123")!!.feeMinor)
        assertEquals(1, count("fingerprint_conflicts"))
    }
}
