"""Regression guards for the R3 release-shrinking experiment."""

from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile, ZIP_STORED

from tools.r3_release_size import compare


ROOT = Path(__file__).resolve().parents[1]


class R3ReleaseShrinkingConfigurationTest(unittest.TestCase):
    def test_release_build_explicitly_shrinks_code_and_resources(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        release = gradle.split('getByName("release") {', 1)[1].split("\n        }", 1)[0]

        self.assertIn("isMinifyEnabled = true", release)
        self.assertIn("isShrinkResources = true", release)
        self.assertIn('"proguard-rules.pro"', release)

    def test_reproducible_release_size_probe_uses_unsigned_apks(self):
        workflow = (ROOT / ".github/workflows/r3-release-size.yml").read_text()

        self.assertIn("pull_request:", workflow)
        self.assertIn("assembleRelease", workflow)
        self.assertIn("github.event.pull_request.base.sha", workflow)
        self.assertIn("github.event.pull_request.head.sha", workflow)
        self.assertIn("git merge-base --is-ancestor", workflow)
        self.assertIn("tools/r3_release_size.py", workflow)
        self.assertNotIn("PESATRACK_RELEASE_KEYSTORE", workflow)

    def test_size_probe_accepts_smaller_archive_with_identical_python_assets(self):
        with tempfile.TemporaryDirectory() as directory:
            baseline = Path(directory) / "baseline.apk"
            minified = Path(directory) / "minified.apk"
            self._synthetic_apk(baseline, 800)
            self._synthetic_apk(minified, 250)

            result = compare(baseline, minified)

            self.assertGreater(result["saved_bytes"], 0)
            self.assertEqual(result["abis"], ["arm64-v8a"])
            self.assertEqual(result["chaquopy_asset_count"], 1)
            self.assertEqual(result["device_acceptance"], "NOT RUN")

    def test_size_probe_rejects_missing_python_assets(self):
        with tempfile.TemporaryDirectory() as directory:
            baseline = Path(directory) / "baseline.apk"
            minified = Path(directory) / "minified.apk"
            self._synthetic_apk(baseline, 600)
            self._synthetic_apk(minified, 250, python_assets=False)

            with self.assertRaisesRegex(ValueError, "Chaquopy"):
                compare(baseline, minified)

    @staticmethod
    def _synthetic_apk(path: Path, padding: int, *, python_assets: bool = True) -> None:
        # Invented ZIP fixtures only; these are not installable APK files.
        with ZipFile(path, "w", compression=ZIP_STORED) as archive:
            archive.writestr("lib/arm64-v8a/libinvented.so", b"native")
            if python_assets:
                archive.writestr("assets/chaquopy/app.zip", b"invented-module")
            archive.writestr("assets/invented-filler", b"x" * padding)


if __name__ == "__main__":
    unittest.main()
