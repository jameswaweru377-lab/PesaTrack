"""Static safeguards for PesaTrack's optional security Actions workflows."""

from pathlib import Path
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"


def load_workflow(name: str):
    path = WORKFLOWS / name
    assert path.is_file(), f"Required security workflow is missing: {name}"
    with path.open(encoding="utf-8") as stream:
        # BaseLoader preserves the GitHub `on` key (YAML 1.2 boolean handling).
        return yaml.load(stream, Loader=yaml.BaseLoader)


class SecurityWorkflowTest(unittest.TestCase):
    def test_manual_signed_release_is_main_only(self):
        workflow = load_workflow("release-build.yml")
        self.assertIn("workflow_dispatch", workflow["on"])
        release = workflow["jobs"]["release"]
        guard = release["if"]
        self.assertIn("github.event_name == 'workflow_dispatch'", guard)
        self.assertIn("github.ref == 'refs/heads/main'", guard)
        self.assertIn("needs.validate_build.result == 'success'", guard)
        self.assertIn("needs.validate_android.result == 'success'", guard)
        self.assertIn("needs.validate_android.result == 'skipped'", guard)

    def test_rolling_apk_publisher_remains_main_push_only(self):
        workflow = load_workflow("android-ci.yml")
        publisher = workflow["jobs"]["publish"]
        self.assertIn("github.event_name == 'push'", publisher["if"])
        self.assertIn("github.ref == 'refs/heads/main'", publisher["if"])

    def test_gitleaks_is_only_on_pull_requests_and_bounded(self):
        workflow = load_workflow("security-gitleaks.yml")
        self.assertEqual(set(workflow["on"]), {"pull_request"})
        self.assertEqual(workflow["permissions"]["contents"], "read")
        job = workflow["jobs"]["secrets"]
        self.assertLessEqual(int(job["timeout-minutes"]), 10)
        steps = job["steps"]
        self.assertEqual(steps[0]["with"]["fetch-depth"], "0")
        commands = "\n".join(step.get("run", "") for step in steps)
        self.assertIn("gitleaks_8.30.1_linux_x64.tar.gz", commands)
        self.assertIn("sha256sum --check", commands)
        self.assertIn('git --no-banner --redact=100', commands)
        self.assertIn('--log-opts="$BASE_SHA..$HEAD_SHA"', commands)
        self.assertNotIn("gitleaks/gitleaks-action", str(steps))

    def test_codeql_is_scheduled_and_manual_with_real_kotlin_build(self):
        workflow = load_workflow("security-codeql.yml")
        self.assertEqual(set(workflow["on"]), {"schedule", "workflow_dispatch"})
        self.assertEqual(workflow["permissions"]["security-events"], "write")
        self.assertLessEqual(int(workflow["jobs"]["codeql"]["timeout-minutes"]), 30)
        steps = workflow["jobs"]["codeql"]["steps"]
        init = next(step for step in steps if step.get("name") == "Initialize CodeQL")
        self.assertEqual(init["with"]["languages"], "java-kotlin")
        self.assertEqual(init["with"]["build-mode"], "manual")
        self.assertTrue(any(":app:compileDebugKotlin" in step.get("run", "") for step in steps))

    def test_mobsf_is_manual_and_reuses_signed_release_apk(self):
        workflow = load_workflow("security-mobsf.yml")
        self.assertEqual(set(workflow["on"]), {"workflow_dispatch"})
        dispatch = workflow["on"]["workflow_dispatch"]
        self.assertIsInstance(dispatch, dict, "MobSF needs a public-report disclosure choice")
        self.assertEqual(dispatch["inputs"]["include_detailed_report"]["default"], "false")
        self.assertEqual(workflow["permissions"]["contents"], "read")
        steps = workflow["jobs"]["apk_scan"]["steps"]
        report_upload = next(step for step in steps if "upload-artifact" in step.get("uses", ""))
        self.assertIn("inputs.include_detailed_report", report_upload["if"])
        commands = "\n".join(step.get("run", "") for step in steps)
        self.assertIn("gh release download ci-apk", commands)
        self.assertIn('--repo "$GITHUB_REPOSITORY"', commands)
        self.assertIn("PesaTrack-Full.apk", commands)
        self.assertIn("127.0.0.1:8000:8000", commands)
        self.assertIn("opensecurity/mobile-security-framework-mobsf@sha256:", commands)
        self.assertNotIn("mobile-security-framework-mobsf:latest", commands)
        self.assertIn("/api/v1/report_json", commands)
        self.assertIn(".appsec.security_score", commands)
        self.assertIn(".appsec.high | length", commands)
        self.assertIn(".appsec.warning | length", commands)
        self.assertNotIn("./gradlew", commands)
        self.assertNotIn("PESATRACK_RELEASE_KEYSTORE", str(workflow))


if __name__ == "__main__":
    unittest.main()
