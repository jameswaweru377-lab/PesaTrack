#!/usr/bin/env bash
set -euo pipefail

home_file="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeScreen.kt"

for stage in home.budgets home.debts home.accountOwnership home.salaryRules home.upcomingBills home.supplementalInsight home.supplementalReviews home.supplementalHealth; do
  if ! grep -Fq "traceFinancialRead(\"$stage\")" "$home_file"; then
    echo "FAIL: missing measurement stage $stage"
    exit 1
  fi
done

grep -Fq 'Home core ready in ' "$home_file" || { echo 'FAIL: no core-to-first-content timing'; exit 1; }
echo 'PASS: Home timing stages are instrumented'
