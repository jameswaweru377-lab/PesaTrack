"""Behavioral tests for the opt-in PesaTrack UI consistency guard."""

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from tools.ui_consistency_guard import scan_added_ui_lines


UI_FILE = "app/src/main/java/com/kedevjimmy/pesatrack/ui/components/Demo.kt"


def sample_diff(*lines, filename=UI_FILE):
    old_count = sum(line.startswith(("-", " ")) for line in lines)
    new_count = sum(line.startswith(("+", " ")) for line in lines)
    return "\n".join(
        [
            f"diff --git a/{filename} b/{filename}",
            f"--- a/{filename}",
            f"+++ b/{filename}",
            f"@@ -1,{old_count} +2,{new_count} @@",
            *lines,
            "",
        ]
    )


class UiConsistencyGuardTest(unittest.TestCase):
    def test_reports_new_hardcoded_color_with_file_and_new_line(self):
        violations = scan_added_ui_lines(sample_diff("+val bad = Color(0xFFAA00BB)"))
        self.assertEqual(1, len(violations))
        self.assertEqual(UI_FILE, violations[0].file)
        self.assertEqual(2, violations[0].line)
        self.assertEqual("raw-color", violations[0].rule)

    def test_reports_one_off_corner_radius_not_tokenized(self):
        violations = scan_added_ui_lines(sample_diff("+val bad = RoundedCornerShape(21.dp)"))
        self.assertEqual(1, len(violations))
        self.assertEqual("raw-corner-radius", violations[0].rule)

    def test_ignores_removed_unchanged_and_comment_lines(self):
        violations = scan_added_ui_lines(
            sample_diff(
                "-val removed = Color(0xFF000000)",
                " val legacy = Color(0xFFFFFFFF)",
                "+// TODO: Color(0xFF112233) is legacy",
            )
        )
        self.assertEqual([], violations)

    def test_ignores_token_usage_and_unrelated_files(self):
        clean = sample_diff(
            "+val bubble = RoundedCornerShape(PesaDesignTokens.iconRadius)",
            "+val text = MaterialTheme.colorScheme.primary",
        )
        unrelated = sample_diff(
            "+val bad = Color(0xFF123456)",
            filename="app/src/main/java/com/kedevjimmy/pesatrack/domain/NoUi.kt",
        )
        self.assertEqual([], scan_added_ui_lines(clean + unrelated))

    def test_rejects_malformed_diff_instead_of_reporting_clean(self):
        with self.assertRaises(ValueError):
            scan_added_ui_lines("this is not a git diff")

    def test_rejects_header_without_hunk_instead_of_silently_passing(self):
        invalid = sample_diff("+val bad = Color(0xFF123456)").replace(
            "@@ -1,0 +2,1 @@\n", ""
        )
        with self.assertRaises(ValueError):
            scan_added_ui_lines(invalid)

    def test_rejects_truncated_hunk_instead_of_reporting_clean(self):
        invalid = sample_diff("+val bad = Color(0xFF123456)").replace(
            "@@ -1,0 +2,1 @@", "@@ -1,0 +2,2 @@"
        )
        with self.assertRaises(ValueError):
            scan_added_ui_lines(invalid)

    def test_detects_named_corner_arguments_and_multiline_colors(self):
        violations = scan_added_ui_lines(sample_diff(
            "+val rounded = RoundedCornerShape(topStart = 21.dp, topEnd = 23.dp)",
            "+val custom = Color(",
            "+    0xFF123456",
            "+)",
        ))
        self.assertEqual(["raw-corner-radius", "raw-color"], [v.rule for v in violations])
        self.assertEqual([2, 3], [v.line for v in violations])

    def test_cli_invalid_base_is_a_failure_even_in_report_mode(self):
        script = Path(__file__).with_name("ui_consistency_guard.py")
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(["git", "init", "-q"], cwd=directory, check=True)
            result = subprocess.run(
                [sys.executable, str(script), "--base", "does-not-exist"],
                cwd=directory,
                capture_output=True,
                text=True,
                check=False,
            )
        self.assertEqual(2, result.returncode)
        self.assertIn("Could not inspect", result.stderr)

    def test_cli_reports_new_untracked_ui_file_without_silently_passing(self):
        script = Path(__file__).with_name("ui_consistency_guard.py")
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(["git", "init", "-q"], cwd=directory, check=True)
            subprocess.run(["git", "-c", "user.name=UI Test", "-c", "user.email=ui@example.test", "commit", "--allow-empty", "-qm", "base"], cwd=directory, check=True)
            target = Path(directory, UI_FILE)
            target.parent.mkdir(parents=True)
            target.write_text("val accent = Color(0xFF123456)\n", encoding="utf-8")

            strict = subprocess.run(
                [sys.executable, str(script), "--base", "HEAD", "--strict"],
                cwd=directory, capture_output=True, text=True, check=False,
            )
            report = subprocess.run(
                [sys.executable, str(script), "--base", "HEAD"],
                cwd=directory, capture_output=True, text=True, check=False,
            )
        self.assertEqual(1, strict.returncode)
        self.assertEqual(0, report.returncode)
        self.assertIn(f"{UI_FILE}:1: raw-color", strict.stdout)


if __name__ == "__main__":
    unittest.main()
