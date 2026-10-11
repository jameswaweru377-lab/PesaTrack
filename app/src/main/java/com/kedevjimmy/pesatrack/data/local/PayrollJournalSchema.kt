package com.kedevjimmy.pesatrack.data.local

/** Additive journal only. These statements never rewrite the canonical cash tables. */
internal object PayrollJournalSchema {
    val statements = listOf(
        """
        CREATE TABLE IF NOT EXISTS payroll_records (
            id TEXT PRIMARY KEY NOT NULL CHECK(length(id) BETWEEN 1 AND 64),
            revision INTEGER NOT NULL CHECK(typeof(revision)='integer' AND revision > 0),
            employer TEXT NOT NULL CHECK(length(trim(employer)) BETWEEN 1 AND 160),
            payroll_month TEXT NOT NULL CHECK(length(payroll_month)=7 AND payroll_month GLOB '[12][0-9][0-9][0-9]-[01][0-9]' AND substr(payroll_month,1,4) BETWEEN '1900' AND '2199' AND substr(payroll_month,6,2) BETWEEN '01' AND '12'),
            gross_minor INTEGER NOT NULL CHECK(typeof(gross_minor)='integer' AND gross_minor > 0),
            net_minor INTEGER NOT NULL CHECK(typeof(net_minor)='integer' AND net_minor >= 0 AND net_minor <= gross_minor),
            source TEXT NOT NULL CHECK(source IN ('MANUAL','PDF_TEXT')),
            period_key TEXT NOT NULL UNIQUE CHECK(length(period_key)=64 AND period_key NOT GLOB '*[^0-9a-f]*'),
            content_key TEXT NOT NULL CHECK(length(content_key)=64 AND content_key NOT GLOB '*[^0-9a-f]*'),
            created_at_epoch_ms INTEGER NOT NULL CHECK(typeof(created_at_epoch_ms)='integer' AND created_at_epoch_ms > 0),
            updated_at_epoch_ms INTEGER NOT NULL CHECK(typeof(updated_at_epoch_ms)='integer' AND updated_at_epoch_ms > 0),
            reviewed INTEGER NOT NULL CHECK(reviewed=1)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS payroll_lines (
            payroll_id TEXT NOT NULL,
            position INTEGER NOT NULL CHECK(typeof(position)='integer' AND position BETWEEN 0 AND 99),
            source_label TEXT NOT NULL CHECK(length(trim(source_label)) BETWEEN 1 AND 160),
            semantic_type TEXT NOT NULL CHECK(semantic_type IN ('TAX','HEALTH','NSSF','PENSION','SACCO_SHARES','SACCO_DEPOSIT','INSURANCE','LOAN_PRINCIPAL','LOAN_INTEREST','OTHER')),
            role TEXT NOT NULL CHECK(role IN ('EMPLOYEE_DEDUCTION','EMPLOYER_CONTRIBUTION')),
            amount_minor INTEGER NOT NULL CHECK(typeof(amount_minor)='integer' AND amount_minor > 0),
            PRIMARY KEY(payroll_id,position),
            FOREIGN KEY(payroll_id) REFERENCES payroll_records(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
        )
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS trg_delete_payroll_lines AFTER DELETE ON payroll_records
        BEGIN DELETE FROM payroll_lines WHERE payroll_id=OLD.id; END
        """.trimIndent()
    )
}
