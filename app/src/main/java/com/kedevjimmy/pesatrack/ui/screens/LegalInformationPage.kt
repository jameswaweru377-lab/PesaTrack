package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/**
 * Explicit PRE-RELEASE disclosures, not a claimed executed user contract.
 * Replace with lawyer-approved, versioned policies and publisher/contact details
 * before requesting legally binding acceptance or shipping a public Play release.
 */
internal enum class LegalNotice { TERMS, PRIVACY, FINANCIAL }

private data class LegalParagraph(val heading: String, val copy: String)

@Composable
internal fun LegalInformationPage(
    padding: PaddingValues,
    back: () -> Unit,
    notice: LegalNotice
) {
    val heading = when (notice) {
        LegalNotice.TERMS -> "Terms of Service"
        LegalNotice.PRIVACY -> "Privacy Policy"
        LegalNotice.FINANCIAL -> "Financial information notice"
    }
    val sections = when (notice) {
        LegalNotice.TERMS -> listOf(
            LegalParagraph("1. What PesaTrack does",
                "PesaTrack is a personal money-organising app for reviewing supported financial messages, imported records, budgets, savings, bills, and user-confirmed debts. It is not a bank, lender, payment provider, debt collector or licensed financial adviser."),
            LegalParagraph("2. Accuracy and financial decisions",
                "Financial records and estimates can be incomplete, delayed, duplicated or interpreted incorrectly. Verify important amounts, balances, names and dates using your official financial service records. This app cannot guarantee financial outcomes."),
            LegalParagraph("3. Debts and evidence",
                "A transfer to someone is not proof of a loan. You choose whether a payment was lent, a gift or a repayment. A PesaTrack debt entry alone does not prove a legally enforceable obligation."),
            LegalParagraph("4. Your responsibilities",
                "Protect your phone and app-lock PIN; check imported records and updates; use other people's information lawfully; and retain independent copies of important financial evidence."),
            LegalParagraph("5. Optional permissions",
                "You may decline historical SMS, financial notification, microphone and online merchant-lookup features. Disabling a permission can limit some functions. See the Privacy Policy for data handling."),
            LegalParagraph("6. Backups and availability",
                "You choose where to save encrypted exports and must retain your passphrase. Uninstalling, deleting files, changing devices or forgetting a passphrase can cause data loss. Third-party message formats or operating system changes can affect functionality."),
            LegalParagraph("7. Limits that respect your rights",
                "Where legally permitted, the app provider does not guarantee uninterrupted service or compensate for decisions made solely from unverified app estimates. No clause excludes liability or Kenyan consumer/data-protection rights that cannot lawfully be excluded."),
            LegalParagraph("8. Changes and disputes",
                "Material changes must be disclosed when required. Applicable Kenyan law and mandatory consumer protections govern disputes. An operator legal name, support channel, full notices and any complaint process must be finalized before public release.")
        )
        LegalNotice.PRIVACY -> listOf(
            LegalParagraph("Data kept on your phone",
                "With your choices and Android permissions, PesaTrack handles financial SMS, approved financial notifications, imported statements, names and counterparties, transaction details, budgets, goals, bills, debts, salary data, settings and optional financial-health inputs. The primary databases are app-private, but are not independently encrypted with SQLCipher."),
            LegalParagraph("SMS and notification permissions",
                "Historical SMS reading is optional. The app looks for financial activity and uses your selected history range. Notification access is a separate Android permission. You can decline or revoke either; already saved local records remain until deleted."),
            LegalParagraph("Online merchant-name lookup",
                "Off by default. If enabled, a sanitized business descriptor and a random installation-scoped rate-limit identifier may be sent to the configured online service. Full SMS bodies, amounts, balances, account numbers, phone numbers and transaction references are not intended to be sent. Filters reduce risk but cannot guarantee that every descriptor is harmless."),
            LegalParagraph("Voice input",
                "Optional voice features request microphone access and use an available on-device Android speech recognizer. You can type instead and review before saving."),
            LegalParagraph("Storage, export, and retention",
                "App-private records remain on the device until removed through app or Android storage controls, subject to device behaviour. Encrypted backups and exports are saved only where you choose; delete those copies separately. Android automatic app backup is disabled."),
            LegalParagraph("Data rights and contact",
                "Kenyan law may grant rights to be informed, access, correct, object to or request deletion of personal data. The release publisher must provide a working private privacy contact and determine any applicable ODPC registration obligation before public launch."),
            LegalParagraph("Security and changes",
                "Optional PIN lock and encrypted user-initiated backups reduce risk; the PIN lock does not encrypt the primary transaction database. No system can guarantee absolute security. Material changes must be reflected in a dated public Privacy Policy before release.")
        )
        LegalNotice.FINANCIAL -> listOf(
            LegalParagraph("Personal information only",
                "PesaTrack is a local-first money tracking tool. It does not move funds, issue loans, provide debt collection, deliver a credit score, or supply regulated investment, tax or legal advice."),
            LegalParagraph("Transaction accuracy",
                "SMS and PDF parsing can misidentify people, miss entries, duplicate transfers or infer the wrong transaction category. Statement archives and Activity may contain overlapping evidence for the same cash movement. Verify against official M-Pesa or bank records."),
            LegalParagraph("Balances and financial-health indicators",
                "The app's displayed balances, forecasts, spending and financial-health metrics are estimates based on available records; they may differ from real-time bank, M-Pesa, loan or savings balances."),
            LegalParagraph("Loans and other people's data",
                "A money transfer is not automatically a personal loan. Review the recipient, purpose, amount and repayment evidence before recording a debt. Records in this app do not independently establish a legal right to demand repayment."),
            LegalParagraph("Keep official documents",
                "Retain official transaction receipts, account statements and any applicable written loan agreements. Do not rely on PesaTrack as the only record for disputes, taxes or high-value financial decisions.")
        )
    }
    EverydayPage(heading, padding, back) {
        item {
            PesaCard(Modifier.fillMaxWidth()) {
                Text(
                    "PRE-RELEASE DRAFT — These summaries are informational, not final legally binding terms. Publisher details, the public privacy contact and lawyer review are required before release.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(sections.size) { index ->
            val section = sections[index]
            PesaCard(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(Modifier.padding(15.dp)) {
                    Text(section.heading, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Text(section.copy, modifier = Modifier.padding(top = 7.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
