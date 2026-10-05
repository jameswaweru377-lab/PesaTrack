package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.planning.BillSchedules
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConceptLedgerIntegrationTest {
    private lateinit var context:Context
    @Before fun setup(){context=ApplicationProvider.getApplicationContext();PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db");context.getSharedPreferences("bill_schedules",0).edit().clear().commit()}
    @After fun cleanup(){PesaTrackDatabase.resetForTests();context.deleteDatabase("pesatrack.db")}
    private fun insert(code:String,amount:Long,rule:SmartRule?,direction:MoneyDirection,category:TransactionCategory,review:ReviewState=ReviewState.USER_CLASSIFIED) {
        val now=System.currentTimeMillis()
        TransactionStore.get(context).insertIfNew(ParsedMpesaTransaction(code,MpesaTransactionType.OTHER,amount,"Example",feeMinor=0,balanceMinor=null,occurredAtText=null,rawMessage=code),null,"TEST",now,now,rule,accountIdOverride=AccountStore.DEFAULT_MPESA_ID,directionOverride=direction,categoryOverride=category,reviewStateOverride=review)
    }
    @Test fun reportsExposeExactlyTrustedRowsAndBillsUseMatchingRuleOnly() {
        val rules=RuleStore.get(context)
        val rent=rules.save("Rent","Example",TransactionCategory.RENT,MoneyDirection.EXPENSE,plannedAmountMinor=1000000)
        val internet=rules.save("Internet","ISP",TransactionCategory.INTERNET,MoneyDirection.EXPENSE,plannedAmountMinor=200000)
        insert("RENT001",400000,rent,MoneyDirection.EXPENSE,TransactionCategory.RENT)
        insert("EXCLUDED001",900000,null,MoneyDirection.EXPENSE,TransactionCategory.OTHER,ReviewState.EXCLUDED)
        val snapshot=TransactionStore.get(context).activitySnapshot()
        assertEquals(listOf("RENT001"),snapshot.trustedTransactions.map {it.transactionCode})
        assertEquals(400000L,snapshot.summary.spentMinor)
        val bills=BillSchedules(context).list().associateBy {it.rule.id}
        assertEquals(600000L,bills[rent.id]!!.remainingMinor)
        assertEquals(200000L,bills[internet.id]!!.remainingMinor)
        BillSchedules(context).save(rent.id,31,false)
        assertEquals(31,BillSchedules(context).list().first {it.rule.id==rent.id}.dueDay)
    }
    @Test fun savingsWithdrawalAndInternalLockMovementAreNotNewSavings() {
        insert("TRANSFER001",100000,null,MoneyDirection.TRANSFER,TransactionCategory.SAVINGS)
        val row=TransactionStore.get(context).activitySnapshot().trustedTransactions.single()
        val types=mapOf("wallet" to AccountType.MOBILE_WALLET,"savings" to AccountType.SAVINGS,"locked" to AccountType.SAVINGS)
        assertEquals(100000L,ConceptLedgerPolicy.savingsDelta(row.copy(accountId="wallet",linkedAccountId="savings"),types))
        assertEquals(-100000L,ConceptLedgerPolicy.savingsDelta(row.copy(accountId="savings",linkedAccountId="wallet"),types))
        assertEquals(0L,ConceptLedgerPolicy.savingsDelta(row.copy(accountId="savings",linkedAccountId="locked"),types))
    }
}
