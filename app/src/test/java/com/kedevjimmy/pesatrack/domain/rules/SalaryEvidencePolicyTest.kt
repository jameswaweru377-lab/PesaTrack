package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class SalaryEvidencePolicyTest {
    private val salary=SmartRule("salary","BRAEBURN",TransactionCategory.INCOME,direction=MoneyDirection.INCOME,preset=RulePreset.SALARY,startsPayCycle=true)
    private fun receipt(type:MpesaTransactionType,body:String)=ParsedMpesaTransaction(null,type,5000,"BRAEBURN SCHOOLS LIMITED CLUB for account coffee shop",feeMinor=0,balanceMinor=null,occurredAtText=null,rawMessage=body)
    @Test fun employerMerchantPurchaseCannotMatchSalary() {
        assertNull(SmartRuleEngine.matchTransaction(receipt(MpesaTransactionType.PAY_BILL,"Ksh50 paid to BRAEBURN SCHOOLS LIMITED CLUB"),listOf(salary)))
        assertNull(SmartRuleEngine.matchTransaction(receipt(MpesaTransactionType.BUY_GOODS,"Ksh50 paid to BRAEBURN"),listOf(salary)))
    }
    @Test fun realIncomingSalaryStillMatches() {
        assertEquals(salary,SmartRuleEngine.matchTransaction(receipt(MpesaTransactionType.RECEIVED,"You have received Ksh60000 from BRAEBURN"),listOf(salary)))
        assertEquals(salary,SmartRuleEngine.matchTransaction(receipt(MpesaTransactionType.OTHER,"Your account has been credited by BRAEBURN"),listOf(salary)))
    }
}
