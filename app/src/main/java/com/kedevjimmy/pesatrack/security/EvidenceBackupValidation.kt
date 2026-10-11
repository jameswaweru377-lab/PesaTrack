package com.kedevjimmy.pesatrack.security

import com.google.gson.JsonObject

/** Refuse malformed linked-source snapshots before restore clears a valid local ledger. */
internal object EvidenceBackupValidation {
    private val digest = Regex("[0-9a-f]{64}")
    private val sourceKinds = setOf("SMS","NOTIFICATION","MANUAL","STATEMENT")
    private val statuses = setOf("PENDING","LINKED","REJECTED")
    private fun text(row: JsonObject, key: String): String {
        val cell = row.getAsJsonObject(key) ?: error("Missing evidence field.")
        require(cell.get("type")?.asString=="string")
        return cell.get("value").asString
    }
    private fun number(row: JsonObject, key: String): Long {
        val cell = row.getAsJsonObject(key) ?: error("Missing evidence number.")
        require(cell.get("type")?.asString=="integer")
        return requireNotNull(cell.get("value")?.asString?.toLongOrNull()) { "Invalid evidence number." }
    }
    fun validate(tables: JsonObject) {
        val accounts=tables.getAsJsonArray("money_accounts").map {text(it.asJsonObject,"id")}.toSet()
        val cashIds=tables.getAsJsonArray("transactions").map {number(it.asJsonObject,"id")}.toSet()
        val observations=linkedMapOf<String,String>()
        tables.getAsJsonArray("evidence_observations").forEach { element ->
            val row=element.asJsonObject
            val id=text(row,"id")
            require(digest.matches(id) && id !in observations)
            require(text(row,"source_kind") in sourceKinds)
            for(key in listOf("source_scope_hash","source_key_hash","fact_hash"))
                require(digest.matches(text(row,key)))
            require(text(row,"account_id") in accounts)
            require(text(row,"direction") in setOf("INCOME","EXPENSE"))
            require(number(row,"amount_minor")>0 && number(row,"occurred_at_epoch_ms")>0 &&
                number(row,"created_at_epoch_ms")>0)
            val ref=row.getAsJsonObject("reference_hash")
            if (ref?.get("type")?.asString=="string") require(digest.matches(ref.get("value").asString))
            else require(ref?.get("type")?.asString=="null")
            val status=text(row,"status")
            require(status in statuses)
            observations[id]=status
        }
        val linked=mutableSetOf<String>()
        tables.getAsJsonArray("evidence_links").forEach { element ->
            val row=element.asJsonObject
            val id=text(row,"observation_id")
            require(observations[id]=="LINKED" && linked.add(id)) { "Invalid evidence link." }
            require(number(row,"transaction_id") in cashIds)
            require(text(row,"reason") in setOf("CREATED","REFERENCE"))
            require(digest.matches(text(row,"transaction_snapshot_hash")))
            require(number(row,"created_at_epoch_ms")>0)
        }
        require(observations.all { (id,status) -> (status=="LINKED")== (id in linked) })
        val auditIds=mutableSetOf<Long>()
        tables.getAsJsonArray("evidence_link_audit").forEach { element ->
            val row=element.asJsonObject
            require(number(row,"id")>0 && auditIds.add(number(row,"id")))
            require(digest.matches(text(row,"observation_id")))
            require(text(row,"action") in setOf("LINKED","UNLINKED"))
            require(text(row,"reason").isNotBlank() && number(row,"created_at_epoch_ms")>0)
        }
    }
}
