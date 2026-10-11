package com.kedevjimmy.pesatrack.security

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceBackupValidationTest {
    private fun cell(value: Any?): JsonObject = JsonObject().apply {
        addProperty("type",when(value) {null->"null";is Number->"integer";else->"string"})
        if(value!=null) {
            if(value is Number)addProperty("value",value)
            else addProperty("value",value.toString())
        }
    }
    private fun row(vararg fields: Pair<String,Any?>): JsonObject = JsonObject().apply {
        fields.forEach {(key,value)->add(key,cell(value))}
    }
    private fun arrays(): JsonObject = JsonObject().apply {
        for(name in listOf("money_accounts","transactions","evidence_observations",
            "evidence_links","evidence_link_audit"))add(name,JsonArray())
    }
    private val sha="a".repeat(64)
    private fun linked(): JsonObject = arrays().apply {
        getAsJsonArray("money_accounts").add(row("id" to "account-mpesa"))
        getAsJsonArray("transactions").add(row("id" to 1))
        getAsJsonArray("evidence_observations").add(row(
            "id" to sha,"source_kind" to "STATEMENT","source_scope_hash" to sha,
            "source_key_hash" to sha,"fact_hash" to sha,"account_id" to "account-mpesa",
            "direction" to "INCOME","amount_minor" to 5_200_000,
            "occurred_at_epoch_ms" to 1_790_000_000_000L,"reference_hash" to null,
            "status" to "LINKED","created_at_epoch_ms" to 1000
        ))
        getAsJsonArray("evidence_links").add(row(
            "observation_id" to sha,"transaction_id" to 1,"reason" to "REFERENCE",
            "transaction_snapshot_hash" to sha,"created_at_epoch_ms" to 1000
        ))
        getAsJsonArray("evidence_link_audit").add(row(
            "id" to 1,"observation_id" to sha,"transaction_id" to 1,
            "action" to "LINKED","reason" to "REFERENCE","created_at_epoch_ms" to 1000
        ))
    }
    @Test fun emptyAndLinkedMetadataRestoresWithoutCreatingCash() {
        EvidenceBackupValidation.validate(arrays())
        EvidenceBackupValidation.validate(linked())
    }
    @Test fun wrongOwnedAccountAndOrphanTargetAreRejected() {
        val wrongAccount=linked()
        wrongAccount.getAsJsonArray("evidence_observations")[0].asJsonObject
            .add("account_id",cell("not-user-owned"))
        assertTrue(runCatching {EvidenceBackupValidation.validate(wrongAccount)}.isFailure)
        val orphan=linked()
        orphan.getAsJsonArray("evidence_links")[0].asJsonObject
            .add("transaction_id",cell(999))
        assertTrue(runCatching {EvidenceBackupValidation.validate(orphan)}.isFailure)
    }
    @Test fun duplicateObservationAndStatusMismatchAreRejected() {
        val duplicate=linked()
        duplicate.getAsJsonArray("evidence_observations").add(
            duplicate.getAsJsonArray("evidence_observations")[0].deepCopy())
        assertTrue(runCatching {EvidenceBackupValidation.validate(duplicate)}.isFailure)
        val mismatch=linked()
        mismatch.getAsJsonArray("evidence_observations")[0].asJsonObject
            .add("status",cell("PENDING"))
        assertTrue(runCatching {EvidenceBackupValidation.validate(mismatch)}.isFailure)
    }
    @Test fun invalidAmountAndUnknownSourceCannotRestore() {
        val bad=linked()
        bad.getAsJsonArray("evidence_observations")[0].asJsonObject
            .add("amount_minor",cell(0))
        assertTrue(runCatching {EvidenceBackupValidation.validate(bad)}.isFailure)
        val source=linked()
        source.getAsJsonArray("evidence_observations")[0].asJsonObject
            .add("source_kind",cell("PAYSLIP"))
        assertTrue(runCatching {EvidenceBackupValidation.validate(source)}.isFailure)
    }
}
