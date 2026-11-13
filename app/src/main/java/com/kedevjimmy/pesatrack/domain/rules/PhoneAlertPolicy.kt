package com.kedevjimmy.pesatrack.domain.rules

/** Destinations and synthetic examples used only when a user explicitly requests a test. */
enum class PhoneAlertType(
    val label: String,
    val route: String,
    val previewTitle: String,
    val previewBody: String
) {
    BILL("Upcoming bill", "bills", "Rent due soon", "Sample property manager • Due in 3 days"),
    SALARY("Salary check", "salary", "Salary received", "Example Bank • KES 45,000"),
    BUDGET("Budget warning", "budgets", "Food budget almost full", "KES 9,500 of KES 10,000 used"),
    REVIEW("Activity review", "activity-attention", "2 transactions need review", "Suggested categories need confirmation"),
    GOAL("Savings goal", "saving-goals", "Savings goal 75% complete", "KES 15,000 saved of KES 20,000"),
    DEBT("Money owed", "debts", "Money owed needs review", "A payment might be connected to money owed"),
    DATA("Data review", "data-health", "Balance needs attention", "Sample Bank balance not yet confirmed"),
    BACKUP("Backup reminder", "backup-reminder", "Keep your records safe", "Your local backup is due for a review")
}

object PhoneAlertPolicy {
    // Existing Android channel importance cannot be raised after creation.
    const val BILL_CHANNEL = "bills_banner_v2"
    const val SALARY_CHANNEL = "salary_banner_v2"
    const val PLAN_CHANNEL = "planning_banner_v2"
    const val BACKUP_CHANNEL = "backup_banner_v1"

    val highVisibilityChannels: List<String> =
        listOf(BILL_CHANNEL, SALARY_CHANNEL, PLAN_CHANNEL, BACKUP_CHANNEL)

    val previewDelaysMinutes: Set<Int> = setOf(0, 1, 10, 20, 30)
    const val defaultPreviewDelayMinutes = 1

    fun previewType(raw: String?): PhoneAlertType? =
        PhoneAlertType.entries.firstOrNull { it.name == raw }
}
