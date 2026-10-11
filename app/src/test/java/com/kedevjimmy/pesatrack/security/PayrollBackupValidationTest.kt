package com.kedevjimmy.pesatrack.security

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kedevjimmy.pesatrack.domain.payroll.*
import org.junit.Assert.*
import org.junit.Test

class PayrollBackupValidationTest {
    private val draft=PayrollDraft("TEST SCHOOL LTD","2026-09",7000000,5200000,PayrollSource.MANUAL,
        listOf(PayrollLine("PAYE",PayrollLineType.TAX,PayrollLineRole.EMPLOYEE_DEDUCTION,1800000)))
    private fun row(vararg entries: Pair<String,Any>): JsonObject = JsonObject().apply { entries.forEach {(key,value)->
        add(key,JsonObject().apply { addProperty("type",if(value is Number) "integer" else "string");
            if(value is Number) addProperty("value",value) else addProperty("value",value.toString()) })
    } }
    private fun tables(): JsonObject = JsonObject().apply {
        add("payroll_records",JsonArray().apply {add(row("id" to "p1","revision" to 1,"employer" to draft.employer,
            "payroll_month" to draft.period,"gross_minor" to draft.grossMinor,"net_minor" to draft.netMinor,
            "source" to "MANUAL","period_key" to PayrollJournalPolicy.periodKey(draft),"content_key" to PayrollJournalPolicy.contentKey(draft),
            "created_at_epoch_ms" to 1000,"updated_at_epoch_ms" to 1000,"reviewed" to 1))})
        add("payroll_lines",JsonArray().apply {add(row("payroll_id" to "p1","position" to 0,"source_label" to "PAYE",
            "semantic_type" to "TAX","role" to "EMPLOYEE_DEDUCTION","amount_minor" to 1800000))})
    }
    private fun mutateLine(field: String,value: Any): JsonObject = tables().apply {
        getAsJsonArray("payroll_lines")[0].asJsonObject.add(field,row(field to value)[field])
    }
    @Test fun validAndEmptyJournalRestoreWithoutCashAuthority() {
        PayrollBackupValidation.validate(tables())
        PayrollBackupValidation.validate(JsonObject().apply {add("payroll_records",JsonArray());add("payroll_lines",JsonArray())})
    }
    @Test fun invalidArithmeticRefusedBeforeRestore() {assertTrue(runCatching {PayrollBackupValidation.validate(mutateLine("amount_minor",1700000))}.isFailure)}
    @Test fun orphanAndMissingPositionsRefusedBeforeRestore() {
        for(t in listOf(mutateLine("payroll_id","absent"),mutateLine("position",1))) assertTrue(runCatching {PayrollBackupValidation.validate(t)}.isFailure)
    }
    @Test fun badHashAndUnreviewedSourceRefused() {
        for(field in listOf("content_key","period_key","reviewed")) {
            val t=tables(); t.getAsJsonArray("payroll_records")[0].asJsonObject.add(field,row(field to if(field=="reviewed") 0 else "bad")[field])
            assertTrue(runCatching {PayrollBackupValidation.validate(t)}.isFailure)
        }
    }
    @Test fun floatAndUnknownKindsRefused() {
        assertTrue(runCatching {PayrollBackupValidation.validate(mutateLine("semantic_type","CASH_RECEIVED"))}.isFailure)
        val t=tables();t.getAsJsonArray("payroll_lines")[0].asJsonObject.getAsJsonObject("amount_minor").addProperty("type","float")
        assertTrue(runCatching {PayrollBackupValidation.validate(t)}.isFailure)
    }
    @Test fun duplicateHeaderAndLineRefused() {
        for(table in listOf("payroll_records","payroll_lines")) {
            val t=tables();val rows=t.getAsJsonArray(table);rows.add(rows[0].deepCopy())
            assertTrue(runCatching {PayrollBackupValidation.validate(t)}.isFailure)
        }
    }
    @Test fun supportedOlderVersionsAddOnlyTablesIntroducedLater() {
        assertEquals(listOf("payroll_records","payroll_lines","evidence_observations","evidence_links","evidence_link_audit"),BackupSchemaCompatibility.emptyTables(38))
        assertTrue(BackupSchemaCompatibility.emptyTables(37).contains("account_ownership_consents"))
        assertTrue(BackupSchemaCompatibility.emptyTables(34).contains("transaction_allocations"))
        assertFalse(BackupSchemaCompatibility.emptyTables(36).contains("personal_receivable_people"))
        assertEquals(listOf("evidence_observations","evidence_links","evidence_link_audit"), BackupSchemaCompatibility.emptyTables(39))
        assertTrue(BackupSchemaCompatibility.emptyTables(40).isEmpty())
        for(version in listOf(0,33,41)) assertTrue(runCatching {BackupSchemaCompatibility.emptyTables(version)}.isFailure)
    }
}
