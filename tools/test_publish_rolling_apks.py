"""Synthetic, offline checks for the two-file signed rolling APK publisher."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PUBLISH = ROOT / "tools" / "publish_rolling_apks.sh"


class RollingApkPublicationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        tools = self.work / "sdk" / "build-tools" / "36.0.0"
        tools.mkdir(parents=True)
        bin_dir = self.work / "bin"
        bin_dir.mkdir()

        for name, content in {
            "apksigner": """#!/bin/sh
case "$4" in
  *PesaTrack-Update.apk) printf 'Signer #1 certificate SHA-256 digest: %s\\n' "${TEST_PREVIOUS_CERT:-abc}" ;;
  *) printf 'Signer #1 certificate SHA-256 digest: abc\\n' ;;
esac
""",
            "aapt": """#!/bin/sh
case "$3" in
  *previous*) number="${TEST_PREVIOUS_VERSION:-10}" ;;
  *) number="${PESATRACK_VERSION_CODE}" ;;
esac
printf "package: name='com.kedevjimmy.pesatrack' versionCode='%s' versionName='0.1.0'\\n" "$number"
printf "sdkVersion:'24'\\ntargetSdkVersion:'36'\\nnative-code: 'arm64-v8a'\\n"
""",
            "zipalign": "#!/bin/sh\nexit 0\n",
        }.items():
            path = tools / name
            path.write_text(content)
            path.chmod(0o755)

        gh = bin_dir / "gh"
        gh.write_text("""#!/usr/bin/env python3
import os, pathlib, shutil, sys
argv = sys.argv[1:]
root = pathlib.Path(os.environ['TEST_GH_STATE'])
root.mkdir(exist_ok=True)
if argv[:2] == ['release', 'view']:
    if '--json' in argv:
        for p in sorted(root.glob('*.apk')):
            print(p.name)
    else:
        sys.exit(0 if (root / 'exists').exists() else 1)
elif argv[:2] == ['release', 'create']:
    (root / 'exists').touch()
elif argv[:2] == ['release', 'download']:
    pattern = argv[argv.index('--pattern') + 1]
    target = pathlib.Path(argv[argv.index('--dir') + 1])
    target.mkdir(exist_ok=True)
    shutil.copy2(root / pattern, target / pattern)
elif argv[:2] == ['release', 'upload']:
    for path in argv[3:]:
        if path.startswith('-'):
            continue
        shutil.copy2(path, root / pathlib.Path(path).name)
elif argv[:2] == ['release', 'delete-asset']:
    (root / argv[3]).unlink()
elif argv[:2] == ['release', 'edit']:
    pass
else:
    sys.exit('unknown gh command: ' + repr(argv))
""")
        gh.chmod(0o755)

        self.release = self.work / "release"
        self.release.mkdir()
        (self.release / "exists").touch()
        self.apk = self.work / "app-release.apk"
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("AndroidManifest.xml", "synthetic, no private data")
        self.env = dict(os.environ)
        self.env.update({
            "ANDROID_HOME": str(self.work / "sdk"),
            "PATH": str(bin_dir) + os.pathsep + self.env.get("PATH", ""),
            "GH_TOKEN": "synthetic",
            "GH_REPO": "example/PesaTrack",
            "TEST_GH_STATE": str(self.release),
            "APK_PATH": str(self.apk),
            "GITHUB_SHA": "0123456789abcdef0123456789abcdef01234567",
            "PESATRACK_VERSION_CODE": "20001",
            "PESATRACK_VERSION_NAME": "0.1.20001",
        })

    def run_publisher(self):
        return subprocess.run(
            ["bash", str(PUBLISH)], env=self.env, cwd=self.work,
            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )

    def test_publishes_exactly_two_identical_assets_and_prunes_history(self):
        (self.release / "PesaTrack-debug-old.apk").write_text("old")
        (self.release / "PesaTrack-debug.apk").write_text("old")
        result = self.run_publisher()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertEqual(
            sorted(path.name for path in self.release.glob("*.apk")),
            ["PesaTrack-Full.apk", "PesaTrack-Update.apk"],
        )
        self.assertEqual((self.release / "PesaTrack-Full.apk").read_bytes(), self.apk.read_bytes())
        self.assertEqual((self.release / "PesaTrack-Update.apk").read_bytes(), self.apk.read_bytes())

    def test_rejects_incompatible_previous_certificate_before_mutating_release(self):
        (self.release / "PesaTrack-Update.apk").write_bytes(self.apk.read_bytes())
        self.env["TEST_PREVIOUS_CERT"] = "different"
        result = self.run_publisher()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("signing certificate", result.stdout)
        self.assertEqual((self.release / "PesaTrack-Update.apk").read_bytes(), self.apk.read_bytes())

    def test_rejects_non_increasing_version_before_mutating_release(self):
        (self.release / "PesaTrack-Update.apk").write_bytes(self.apk.read_bytes())
        self.env["TEST_PREVIOUS_VERSION"] = "20001"
        result = self.run_publisher()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("versionCode", result.stdout)
        self.assertEqual((self.release / "PesaTrack-Update.apk").read_bytes(), self.apk.read_bytes())

    def test_rejects_corrupt_previous_update_before_mutating_release(self):
        (self.release / "PesaTrack-Update.apk").write_text("not an APK")
        result = self.run_publisher()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.release / "PesaTrack-Update.apk").read_text(), "not an APK")

    def test_rejects_missing_version_metadata(self):
        self.env["PESATRACK_VERSION_CODE"] = ""
        result = self.run_publisher()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.release / "PesaTrack-Update.apk").exists())


class RollingWorkflowWiringTest(unittest.TestCase):
    def test_main_publication_uses_release_signing_and_not_debug_attachments(self):
        workflow = (ROOT / ".github" / "workflows" / "android-ci.yml").read_text()
        self.assertIn("bash tools/publish_rolling_apks.sh", workflow)
        self.assertIn("PESATRACK_RELEASE_KEYSTORE_BASE64", workflow)
        self.assertIn("PESATRACK_RELEASE_STORE_PASSWORD", workflow)
        self.assertIn("PESATRACK_RELEASE_KEY_ALIAS", workflow)
        self.assertIn("PESATRACK_RELEASE_KEY_PASSWORD", workflow)
        self.assertIn(":app:assembleRelease", workflow)
        self.assertNotIn("gh release upload ci-apk \\\n            PesaTrack-debug.apk", workflow)
        self.assertNotIn('versioned_apk="PesaTrack-debug-', workflow)
        self.assertIn("python3 -m unittest tools.test_publish_rolling_apks", workflow)


if __name__ == "__main__":
    unittest.main()
