#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
screens="$root/app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"

# The approved pages must not temporarily render an older design while local
# financial data is loading. Real read failures must still offer a retry.
if rg -q 'GreetingBlock|Preparing your money picture|Loading your accounts, pay cycle and balances|CircularProgressIndicator' "$screens/HomeScreen.kt"; then
    echo 'Old Home UI still reachable' >&2
    exit 1
fi
if rg -q 'Loading your activity…' "$screens/ActivityScreen.kt"; then
    echo 'Old Activity loading screen still reachable' >&2
    exit 1
fi
if rg -q 'Loading your plan…' "$screens/PlanScreen.kt"; then
    echo 'Old Plan loading screen still reachable' >&2
    exit 1
fi
rg -Fq 'if (homeError)' "$screens/HomeScreen.kt"
rg -Fq 'if (activityError)' "$screens/ActivityScreen.kt"
rg -Fq 'loadError != null' "$screens/PlanScreen.kt"

# Old full-page Home concepts cannot accidentally be reintroduced by imports.
if rg -q 'fun (HomeBGreeting|HomeBHealth|ReferenceHomeHeader|MonochromeAvailableMoney|HomeTodayCard|HomeMoneyPictureCard)' "$screens"; then
    echo 'Retired Home page implementation remains' >&2
    exit 1
fi

echo 'Retired UI entry contracts passed'
