#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
screens="$root_dir/app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"

# Editorial list sections use their surface colour, not decorative outer strokes.
for screen in ProfileWorkspaceAndSettings.kt InvestmentPortfolioContent.kt AccountsVisualComponents.kt StatementEmptyLanding.kt PlanVibrantComponents.kt; do
    if rg -q 'BorderStroke\(' "$screens/$screen"; then
        echo "Unexpected card outline in $screen" >&2
        exit 1
    fi
done

# The review rows are grouped, remain individually actionable, and are not oversized cards.
rg -Fq 'ActivityReviewGroupList(' "$screens/ActivityScreen.kt"
! rg -q 'private fun ReviewGroupRow\(' "$screens/ActivityScreen.kt"
rg -Fq 'onSelect(group.representativeTransactionId)' "$screens/ActivityReviewGroupList.kt"
rg -Fq 'formatActivityMoney(group.totalMinor)' "$screens/ActivityReviewGroupList.kt"
rg -Fq 'group.latestEpochMs' "$screens/ActivityReviewGroupList.kt"

# Compact account notices keep their critical actions available.
rg -Fq 'AccountBalanceCheckSheet(account.id)' "$screens/BalanceAccountNotice.kt"
rg -Fq 'BalanceArchiveDialog(account, archive = true)' "$screens/BalanceAccountNotice.kt"
rg -Fq '"Add balance  →" else "Update balance  →"' "$screens/BalanceAccountNotice.kt"
! rg -q '"BALANCE CHECK"' "$screens/BalanceAccountNotice.kt"

# Grouped sections use the shared, low-contrast divider treatment.
rg -Fq 'PesaDesignTokens.faintDividerAlpha' "$screens/ProfileWorkspaceAndSettings.kt"
rg -Fq 'PesaDesignTokens.faintDividerAlpha' "$screens/ApprovedAccountsSurface.kt"
rg -Fq 'PesaDesignTokens.faintDividerAlpha' "$screens/ActivityReviewGroupList.kt"

echo 'Surface polish UI source contracts passed'
