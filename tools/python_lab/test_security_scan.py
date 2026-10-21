"""Synthetic regressions for the offline build-time security policy."""
import unittest

from tools.security_scan import inspect_logs, log_arguments


class SecurityScanTest(unittest.TestCase):
    def test_rejects_throwable_log_overload(self):
        self.assertEqual(1, len(inspect_logs(
            'android.util.Log.e("PesaTrackSalary", "Save failed", exception)',
            "SalaryScreen.kt")))

    def test_accepts_two_safe_arguments_even_with_nested_text(self):
        self.assertEqual([], inspect_logs(
            'Log.i("PesaTrackPerf", "Refresh took $duration ms")\n'
            'Log.e("PesaTrackActivity", "Could not load")', "ActivityScreen.kt"))

    def test_ignores_commas_in_strings_and_nested_calls(self):
        self.assertEqual([], inspect_logs(
            'Log.e("tag,a", buildSafeMessage(count(1, 2)))', "Example.kt"))

    def test_rejects_incomplete_and_multiline_exception_logging(self):
        self.assertEqual(1, len(inspect_logs(
            'Log.e(\n  "SensitiveScreen",\n  "Error",\n  cause\n)', "Example.kt")))
        self.assertEqual(999, log_arguments('Log.e("tag",', 5))


if __name__ == "__main__":
    unittest.main()
