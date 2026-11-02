package com.kedevjimmy.pesatrack.domain.rules

/** Destinations and synthetic examples used only when a user explicitly requests a test. */
enum class PhoneAlertType(
    val label: String,
    val route: String,
    val previewTitle: String,
    val previewBody: String
) {
    BILL("Upcoming bill", "bills", "Test: Bill coming up", "A scheduled bill is approaching. Tap to review your bills."),
    SALARY("Salary check", "salary", "Test: Salary check", "Your expected payday has passed. Tap to check your salary."),
    BUDGET("Budget warning", "budgets", "Test: Budget warning", "You're approaching a budget limit. Tap to review your plan."),
    REVIEW("Activity review", "activity-attention", "Test: Activity needs review", "Some transactions may need your attention. Tap to review."),
    GOAL("Savings goal", "saving-goals", "Test: Savings reminder", "It's time to check progress on a savings goal."),
    DEBT("Money owed", "debts", "Test: Money owed", "A payment may need your review. Tap to check money owed."),
    DATA("Data review", "data-health", "Test: Data review", "Something may need confirmation. Tap to check data health."),
    BACKUP("Backup reminder", "backup-reminder", "Test: Backup reminder", "It's a good time to save a private backup of your records.")
}

object PhoneAlertPolicy {
    // Existing Android channel importance cannot be raised after creation.
    const val BILL_CHANNEL = "bills_banner_v2"
    const val SALARY_CHANNEL = "salary_banner_v2"
    const val PLAN_CHANNEL = "planning_banner_v2"
    const val BACKUP_CHANNEL = "backup_banner_v1"

    val highVisibilityChannels: List<String> =
        listOf(BILL_CHANNEL, SALARY_CHANNEL, PLAN_CHANNEL, BACKUP_CHANNEL)

    val previewDelaysMinutes: Set<Int> = setOf(0, 10, 20, 30)

    fun previewType(raw: String?): PhoneAlertType? =
        PhoneAlertType.entries.firstOrNull { it.name == raw }
}
