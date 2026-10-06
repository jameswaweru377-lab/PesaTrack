package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.planning.BillCadence
import com.kedevjimmy.pesatrack.planning.BillSchedules
import java.util.Calendar
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
        val rent=rules.save("Rent","Example",TransactionCategory.RENT,MoneyDirection.EXPENSE,preset=RulePreset.OTHER_RECURRING,plannedAmountMinor=1000000)
        val internet=rules.save("Internet","ISP",TransactionCategory.INTERNET,MoneyDirection.EXPENSE,preset=RulePreset.OTHER_RECURRING,plannedAmountMinor=200000)
        val repeatedButNotABill=rules.save("Coffee","Cafe",TransactionCategory.EATING_OUT,MoneyDirection.EXPENSE,plannedAmountMinor=50000)
        BillSchedules(context).save(rent.id,31,false)
        BillSchedules(context).save(internet.id,15,false)
        insert("RENT001",400000,rent,MoneyDirection.EXPENSE,TransactionCategory.RENT)
        insert("EXCLUDED001",900000,null,MoneyDirection.EXPENSE,TransactionCategory.OTHER,ReviewState.EXCLUDED)
        val snapshot=TransactionStore.get(context).activitySnapshot()
        assertEquals(listOf("RENT001"),snapshot.trustedTransactions.map {it.transactionCode})
        assertEquals(400000L,snapshot.summary.spentMinor)
        val bills=BillSchedules(context).list().associateBy {it.rule.id}
        assertEquals(600000L,bills[rent.id]!!.remainingMinor)
        assertEquals(200000L,bills[internet.id]!!.remainingMinor)
        assertFalse(bills.containsKey(repeatedButNotABill.id))
        assertEquals(31,bills[rent.id]!!.dueDay)
    }
    @Test fun fullyPaidCurrentMonthBillIsNotUpcoming() {
        val rent=RuleStore.get(context).save("Rent","Landlord",TransactionCategory.RENT,MoneyDirection.EXPENSE,preset=RulePreset.OTHER_RECURRING,plannedAmountMinor=1000000)
        BillSchedules(context).save(rent.id,5,false)
        insert("RENT-PAID",1000000,rent,MoneyDirection.EXPENSE,TransactionCategory.RENT)
        assertTrue(BillSchedules(context).upcomingThisMonth().none {it.rule.id==rent.id})
        assertEquals(0L,BillSchedules(context).list().first {it.rule.id==rent.id}.remainingMinor)
    }

    @Test fun legacyTrustedPaymentWithoutRuleIdCanStillMarkExplicitBillPaid() {
        val rent=RuleStore.get(context).save("Rent","Example",TransactionCategory.RENT,MoneyDirection.EXPENSE,preset=RulePreset.OTHER_RECURRING,plannedAmountMinor=1000000)
        BillSchedules(context).save(rent.id,5,false)
        insert("LEGACY-RENT",1000000,null,MoneyDirection.EXPENSE,TransactionCategory.RENT)
        val bill=BillSchedules(context).list().first {it.rule.id==rent.id}
        assertEquals(0L,bill.remainingMinor)
        assertTrue(BillSchedules(context).upcomingThisMonth().none {it.rule.id==rent.id})
    }

    @Test fun weeklyBillUsesOnlyOccurrencesInsideCurrentMonth() {
        val rule=RuleStore.get(context).save("Weekly service","Service",TransactionCategory.OTHER,MoneyDirection.EXPENSE,preset=RulePreset.OTHER_RECURRING,plannedAmountMinor=10000)
        BillSchedules(context).saveSchedule(rule.id,BillCadence.WEEKLY,dueDayOfWeek=Calendar.MONDAY,enabled=false)
        val month=Calendar.getInstance().apply {set(Calendar.DAY_OF_MONTH,1)}
        val targetMonth=month.get(Calendar.MONTH)
        var mondays=0
        while(month.get(Calendar.MONTH)==targetMonth) {
            if(month.get(Calendar.DAY_OF_WEEK)==Calendar.MONDAY) mondays++
            month.add(Calendar.DAY_OF_MONTH,1)
        }
        val bill=BillSchedules(context).list().first {it.rule.id==rule.id}
        assertEquals(mondays*10000L,bill.expectedMinor)
        assertEquals(bill.expectedMinor,bill.remainingMinor)
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
