#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
screens="$root_dir/app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"
app="$root_dir/app/src/main/java/com/kedevjimmy/pesatrack/ui/PesaTrackApp.kt"

! rg -q 'Add goal by voice' "$screens/GoalReferenceScreen.kt"
! rg -q 'App preview' "$screens/ProfileAppearancePage.kt"
! rg -q 'Period in progress' "$screens/ReportsInsightsScreen.kt"
! rg -q 'Available money uses only balances PesaTrack can verify' "$screens/BalanceAccountNotice.kt"
rg -q 'showTopBar = false' "$app"
rg -q 'MoneyGroupingVisualTransformation' "$screens/SavingGoalEditor.kt"
rg -q 'MoneyGroupingVisualTransformation' "$screens/BudgetScreen.kt"
rg -q 'ModalBottomSheet' "$screens/BudgetScreen.kt"
rg -q 'Choose a budget category' "$screens/BudgetScreen.kt"
! rg -q 'if \(categoryExpanded \|\| budgets.isEmpty\(\)\)' "$screens/BudgetScreen.kt"
rg -q 'ThemeMode.entries' "$screens/ProfileAppearancePage.kt"
rg -q 'grouped = true' "$screens/TransactionDetailScreen.kt"
rg -Fq 'TransferReconciliationStore(context).read(overview.year)' "$screens/StatementUnifiedOverviewPage.kt"
rg -q 'reconciliation.pairs.any' "$screens/StatementUnifiedOverviewPage.kt"
echo 'Batch 1 UI source contracts passed'
