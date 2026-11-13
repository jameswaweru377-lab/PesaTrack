package com.kedevjimmy.pesatrack.domain.rules

/** Destinations and synthetic examples used only when a user explicitly requests a test. */
enum class PhoneAlertType(
    val label: String,
    val route: String,
    val previewTitle: String,
    val previewBody: String
) {
    BILL("Upcoming bill", "bills", "Test: Rent due soon", "Sample property manager • Due in 3 days"),
    SALARY("Salary check", "salary", "Test: Salary received", "Example bank • Confirmed income sample"),
    BUDGET("Budget warning", "budgets", "Test: Food budget almost full", "A sample food budget is close to its limit"),
    REVIEW("Activity review", "activity-attention", "Test: 2 transactions need review", "Suggested categories need confirmation"),
    GOAL("Savings goal", "saving-goals", "Test: Savings goal progress", "An example goal is getting close to its target"),
    DEBT("Money owed", "debts", "Test: Money owed needs review", "A payment might be connected to money owed"),
    DATA("Data review", "data-health", "Test: Balance needs attention", "Example bank balance not yet confirmed"),
    BACKUP("Backup reminder", "backup-reminder", "Test: Keep your records safe", "Your local backup is due for a review")
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
