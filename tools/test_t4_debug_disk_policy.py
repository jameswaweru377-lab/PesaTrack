"""Source guards for T4's debug-only main-thread disk I/O warning policy.

These tests check wiring, not runtime StrictMode callbacks on a device.
"""

from pathlib import Path
import re
import unittest


APP_SOURCE = (
    Path(__file__).resolve().parents[1]
    / "app/src/main/java/com/kedevjimmy/pesatrack/PesaTrackApplication.kt"
)


class DebugDiskPolicyWiringTest(unittest.TestCase):
    def setUp(self):
        self.source = APP_SOURCE.read_text(encoding="utf-8")

    def test_debug_only_policy_logs_disk_reads_and_writes(self):
        self.assertRegex(
            self.source,
            r"if\s*\(BuildConfig\.DEBUG\)\s*\{\s*StrictMode\.setThreadPolicy\(",
        )
        policy = self.source.split("StrictMode.setThreadPolicy(", 1)[1]
        for expected in (
            "StrictMode.ThreadPolicy.Builder()",
            ".detectDiskReads()",
            ".detectDiskWrites()",
            ".penaltyLog()",
            ".build()",
        ):
            self.assertIn(expected, policy)
        self.assertNotIn(".penaltyDeath", policy)
        self.assertNotIn(".detectNetwork", policy)

    def test_policy_starts_only_after_isolated_process_and_recovery(self):
        self.assertLess(self.source.index("isIsolated()) return"), self.source.index("BuildConfig.DEBUG"))
        self.assertLess(self.source.index("BackupRecoveryStatus.retry(this)"), self.source.index("BuildConfig.DEBUG"))
        self.assertLess(self.source.index("SmartNoticeLifecycle.start(this)"), self.source.index("BuildConfig.DEBUG"))
        # Installation in Application.onCreate uses Android's main thread.
        self.assertTrue(re.search(r"override fun onCreate\(\)", self.source))


if __name__ == "__main__":
    unittest.main()
