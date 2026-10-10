#!/usr/bin/env bash
# Preserve invented UI evidence even when a regression assertion fails.
set -u
classes="com.kedevjimmy.pesatrack.ui.screens.PrivacyReminderSettingsUiTest,com.kedevjimmy.pesatrack.ingestion.NotificationSourceApprovalUiTest,com.kedevjimmy.pesatrack.ui.OwnershipReviewUiTest,com.kedevjimmy.pesatrack.data.local.OwnershipReviewEvidenceTest,com.kedevjimmy.pesatrack.ui.FinancialReadFailureUiTest,com.kedevjimmy.pesatrack.ui.AuditAccessibilityUiTest,com.kedevjimmy.pesatrack.ui.screens.PlanCacheUiTest,com.kedevjimmy.pesatrack.ui.SmartRuleConsentUiTest"
result=0
./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$classes" --stacktrace || result=$?
adb logcat -d -v raw 'PesaTrackMeasurement:I' 'AuditAccessibility:I' '*:S' > history-measurements.jsonl
mkdir -p app/build/audit-accessibility
adb pull /sdcard/Android/data/com.kedevjimmy.pesatrack/files/audit-accessibility app/build/audit-accessibility/ || true
git rev-parse HEAD > ANDROID_TEST_SOURCE_SHA.txt
exit "$result"
