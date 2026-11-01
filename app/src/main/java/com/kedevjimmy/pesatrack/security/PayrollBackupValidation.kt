package com.kedevjimmy.pesatrack.security

import com.google.gson.JsonObject
import com.kedevjimmy.pesatrack.domain.payroll.*

/** Semantic validation before the restore journal stages or deletes any existing history. */
internal object PayrollBackupValidation {
    fun validate(tables: JsonObject) {
        val records=tables.getAsJsonArray("payroll_records")
        val lines=tables.getAsJsonArray("payroll_lines")
        fun text(row: JsonObject,key: String): String {
            val cell=row.getAsJsonObject(key)
            require(cell.get("type").asString=="string" && cell.get("value").asJsonPrimitive.isString)
            return cell.get("value").asString
        }
        fun number(row: JsonObject,key: String): Long {
            val cell=row.getAsJsonObject(key)
            require(cell.get("type").asString=="integer" && cell.get("value").asJsonPrimitive.isNumber)
            return requireNotNull(cell.get("value").asString.toLongOrNull()) { "Invalid payroll number in backup." }
        }
        val grouped=lines.map {it.asJsonObject}.groupBy {text(it,"payroll_id")}
        val ids=mutableSetOf<String>();val periods=mutableSetOf<String>()
        records.forEach {element ->
            val row=element.asJsonObject;val id=text(row,"id")
            require(id.length in 1..64 && ids.add(id)) { "Duplicate payroll record in backup." }
            require(number(row,"revision")>0 && number(row,"reviewed")==1L &&
                number(row,"created_at_epoch_ms")>0 && number(row,"updated_at_epoch_ms")>0)
            val ordered=grouped[id].orEmpty().sortedBy {number(it,"position")}
            val deductions=ordered.mapIndexed {position,line ->
                require(number(line,"position")==position.toLong()) { "Payroll lines are incomplete or duplicated." }
                PayrollLine(text(line,"source_label"),PayrollLineType.valueOf(text(line,"semantic_type")),
                    PayrollLineRole.valueOf(text(line,"role")),number(line,"amount_minor"))
            }
            val draft=PayrollDraft(text(row,"employer"),text(row,"payroll_month"),number(row,"gross_minor"),
                number(row,"net_minor"),PayrollSource.valueOf(text(row,"source")),deductions)
            PayrollJournalPolicy.validate(draft)
            val key=PayrollJournalPolicy.periodKey(draft)
            require(key==text(row,"period_key") && periods.add(key) && PayrollJournalPolicy.contentKey(draft)==text(row,"content_key")) {
                "Payroll identity or content is inconsistent in this backup."
            }
        }
        require(grouped.keys.all {it in ids}) { "Payroll lines have no record in this backup." }
    }
}

internal object BackupSchemaCompatibility {
    fun emptyTables(version: Int): List<String> {
        require(version in 34..40) { "Unsupported backup schema." }
        return (if(version<41) listOf("fingerprint_conflicts") else emptyList()) +
            (if(version<35) listOf("transaction_allocations","flexible_budget_options","statement_import_entries") else emptyList()) +
            (if(version<36) listOf("personal_receivable_people","personal_receivable_events") else emptyList()) +
            (if(version<37) listOf("personal_receivable_review_exclusions") else emptyList()) +
            (if(version<38) listOf("account_ownership_consents","owned_transfer_review_decisions") else emptyList()) +
            (if(version<39) listOf("payroll_records","payroll_lines") else emptyList()) +
            (if(version<40) listOf("evidence_observations","evidence_links","evidence_link_audit") else emptyList())
    }
}
