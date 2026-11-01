"""The auto-merge gate must reject ambiguous, stale, and non-additive PRs."""

from __future__ import annotations

from copy import deepcopy
from pathlib import Path
from unittest.mock import patch
import unittest

from tools.tracker.restricted_merge import evaluate_candidate, main, run


OWNER = "jameswaweru377-lab"
REPO = f"{OWNER}/PesaTrack"
MAIN = "a" * 40
HEAD = "b" * 40


def safe_fixture():
    pr = {
        "number": 31, "state": "open", "draft": False,
        "user": {"login": OWNER},
        "base": {"ref": "main", "sha": MAIN},
        "head": {"sha": HEAD, "repo": {"full_name": REPO}},
        "mergeable": True, "merge_base_sha": MAIN,
        "labels": [{"name": "pesatrack-safe-automerge"}],
        "body": "Tracks #20\nChat owner: CHAT-DOCS",
    }
    comments = [{"user": {"login": OWNER},
                 "body": f"PESATRACK SAFE AUTO-MERGE APPROVED: {HEAD}"}]
    issue = {"number": 20, "state": "open", "body": "Chat owner: CHAT-DOCS",
             "labels": [{"name": "pesatrack-tracker"}]}
    files = [{"filename": "docs/tracker/new-note.md", "status": "added"},
             {"filename": "tools/tracker/tests/test_new_case.py", "status": "added"}]
    runs = [{"name": name, "head_sha": HEAD, "event": "pull_request",
             "status": "completed", "conclusion": "success", "id": index,
             "run_attempt": 1}
            for index, name in enumerate(("Android CI", "Security - PR Secrets"), 1)]
    return pr, comments, issue, files, [], runs


class RestrictedMergePolicyTest(unittest.TestCase):
    def decide(self, fixture):
        pr, approvals, issue, files, open_pr_files, runs = fixture
        return evaluate_candidate(pr, main_sha=MAIN, approvals=approvals,
                                  issue=issue, files=files,
                                  open_pr_files=open_pr_files, runs=runs)

    def test_approved_added_tracker_docs_and_tests_are_eligible(self):
        self.assertEqual(self.decide(safe_fixture()), (True, "eligible"))

    def test_owner_documentation_only_task_merges_without_manual_approval(self):
        fixture = list(deepcopy(safe_fixture()))
        fixture[0]["labels"] = []
        fixture[1] = []
        fixture[3] = [{"filename": "docs/tracker/2026-11-01-progress.md", "status": "added"}]
        self.assertEqual(self.decide(fixture), (True, "eligible"))

    def test_explicit_paused_blocked_or_untriaged_task_never_merges(self):
        for state in ("Paused", "Blocked", "Superseded", "Untriaged", "Unclaimed"):
            with self.subTest(state=state):
                fixture = list(deepcopy(safe_fixture()))
                fixture[0]["labels"] = []
                fixture[1] = []
                fixture[2]["body"] += f"\nTask status: {state}"
                fixture[3] = [{"filename": "docs/tracker/new-note.md", "status": "added"}]
                self.assertEqual(self.decide(fixture), (False, "task-not-actionable"))

    def test_non_owner_cannot_use_unattended_documentation_lane(self):
        fixture = list(deepcopy(safe_fixture()))
        fixture[0]["user"]["login"] = "another-contributor"
        fixture[0]["labels"] = []
        fixture[1] = []
        fixture[3] = [{"filename": "docs/tracker/new-note.md", "status": "added"}]
        self.assertEqual(self.decide(fixture), (False, "owner-approval-missing"))

    def test_ambiguous_claims_cannot_trigger_unattended_merge(self):
        for body in ("Tracks #20\nTracks #21\nChat owner: CHAT-DOCS",
                     "Tracks #20\nChat owner: CHAT-DOCS\nChat owner: CHAT-OTHER"):
            fixture = list(deepcopy(safe_fixture()))
            fixture[0]["body"] = body
            fixture[0]["labels"] = []
            fixture[1] = []
            fixture[3] = [{"filename": "docs/tracker/new-note.md", "status": "added"}]
            self.assertFalse(self.decide(fixture)[0])

    def test_malformed_author_is_never_an_unattended_candidate(self):
        fixture = list(deepcopy(safe_fixture()))
        fixture[0]["user"] = "malformed"
        fixture[0]["labels"] = []
        fixture[1] = []
        fixture[3] = [{"filename": "docs/tracker/new-note.md", "status": "added"}]
        self.assertFalse(self.decide(fixture)[0])
        client = FakeGitHub()
        client.pr = fixture[0]
        self.assertEqual(run(REPO, "fake", apply=False, client=client), [])

    def test_unattended_lane_rejects_added_executable_or_existing_file_edits(self):
        for path, change in (("tools/tracker/tests/test_new_case.py", "added"),
                             ("docs/tracker/new-note.md", "modified"),
                             ("docs/tracker/new-note.md", "renamed"),
                             (".github/workflows/android-ci.yml", "added")):
            with self.subTest(path=path, change=change):
                fixture = list(deepcopy(safe_fixture()))
                fixture[0]["labels"] = []
                fixture[1] = []
                fixture[3] = [{"filename": path, "status": change}]
                self.assertFalse(self.decide(fixture)[0])

    def test_label_without_exact_owner_approval_never_merges(self):
        for kind in ("missing", "old-sha", "different-user", "missing-label"):
            with self.subTest(kind=kind):
                fixture = list(deepcopy(safe_fixture()))
                if kind == "missing":
                    fixture[1] = []
                if kind == "old-sha":
                    fixture[1][0]["body"] = "PESATRACK SAFE AUTO-MERGE APPROVED: " + "c" * 40
                if kind == "different-user":
                    fixture[1][0]["user"]["login"] = "external-user"
                if kind == "missing-label":
                    fixture[0]["labels"] = []
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)

    def test_stale_main_or_draft_or_fork_is_blocked(self):
        for kind in ("stale", "draft", "fork", "unmergeable", "closed"):
            with self.subTest(kind=kind):
                fixture = list(deepcopy(safe_fixture()))
                pr = fixture[0]
                if kind == "stale":
                    pr["merge_base_sha"] = "f" * 40
                if kind == "draft":
                    pr["draft"] = True
                if kind == "fork":
                    pr["head"]["repo"]["full_name"] = "someone-else/PesaTrack"
                if kind == "unmergeable":
                    pr["mergeable"] = None
                if kind == "closed":
                    pr["state"] = "closed"
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)

    def test_modified_or_dangerous_paths_are_never_eligible(self):
        for filename, status in (("README.md", "added"),
                                 ("docs/RELEASE_PROCESS.md", "added"),
                                 ("app/src/main/java/TransactionStore.kt", "added"),
                                 (".github/workflows/android-ci.yml", "added"),
                                 ("docs/tracker/existing.md", "modified"),
                                 ("app/src/test/../main/Sneaky.kt", "added"),
                                 ("tools/tracker/tests/test_case.py", "renamed")):
            with self.subTest(path=filename, status=status):
                fixture = list(deepcopy(safe_fixture()))
                fixture[3] = [{"filename": filename, "status": status}]
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)

    def test_overlapping_open_pr_is_blocked(self):
        fixture = list(safe_fixture())
        fixture[4] = [{"number": 33, "files": ["docs/tracker/new-note.md"]}]
        self.assertEqual(self.decide(fixture), (False, "overlap"))

    def test_missing_wrong_sha_or_failing_latest_ci_never_merges(self):
        for kind in ("missing", "wrong-sha", "newer-failed", "skipped"):
            with self.subTest(kind=kind):
                fixture = list(deepcopy(safe_fixture()))
                runs = fixture[5]
                if kind == "missing":
                    runs.pop()
                if kind == "wrong-sha":
                    runs[0]["head_sha"] = "e" * 40
                if kind == "newer-failed":
                    runs.append({**runs[0], "id": 99, "conclusion": "failure"})
                if kind == "skipped":
                    runs[1]["conclusion"] = "skipped"
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)

    def test_task_issue_and_owner_must_match(self):
        for kind in ("missing-task", "mismatched-owner", "closed-issue"):
            with self.subTest(kind=kind):
                fixture = list(deepcopy(safe_fixture()))
                if kind == "missing-task":
                    fixture[0]["body"] = "Chat owner: CHAT-DOCS"
                if kind == "mismatched-owner":
                    fixture[2]["body"] = "Chat owner: CHAT-OTHER"
                if kind == "closed-issue":
                    fixture[2]["state"] = "closed"
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)

    def test_executable_android_tests_are_outside_first_stage(self):
        fixture = list(deepcopy(safe_fixture()))
        fixture[3] = [{"filename": "app/src/test/kotlin/Test.kt", "status": "added"}]
        self.assertEqual(self.decide(fixture), (False, "unsafe-path"))

    def test_malformed_head_or_comment_fails_closed(self):
        for kind in ("null-head-repo", "null-comment", "bad-body"):
            with self.subTest(kind=kind):
                fixture = list(deepcopy(safe_fixture()))
                if kind == "null-head-repo":
                    fixture[0]["head"]["repo"] = None
                if kind == "null-comment":
                    fixture[1][0]["body"] = None
                if kind == "bad-body":
                    fixture[0]["body"] = {"unexpected": "shape"}
                allowed, _ = self.decide(fixture)
                self.assertFalse(allowed)


class FakeGitHub:
    def __init__(self, *, conflicting=False, main_moves=False):
        self.pr, self.approvals, self.issue, self.files, _, self.runs = deepcopy(safe_fixture())
        self.conflicting = conflicting
        self.main_moves = main_moves
        self.main_reads = 0
        self.merges = []

    def get(self, path, **query):
        if path == "commits/main":
            self.main_reads += 1
            return {"sha": "c" * 40 if self.main_moves and self.main_reads > 1 else MAIN}
        if path == "pulls":
            return [self.pr, {"number": 40}] if self.conflicting else [self.pr]
        if path == "pulls/31":
            return deepcopy(self.pr)
        if path.startswith("compare/"):
            return {"merge_base_commit": {"sha": MAIN}}
        if path == "issues/31/comments":
            return deepcopy(self.approvals)
        if path == "issues/20":
            return deepcopy(self.issue)
        if path == "pulls/31/files":
            return deepcopy(self.files)
        if path == "pulls/40/files":
            return [{"filename": self.files[0]["filename"], "status": "added"}]
        if path == "actions/runs":
            return {"workflow_runs": deepcopy(self.runs)}
        raise AssertionError(f"Unexpected API lookup: {path}")

    def merge(self, number, expected_head_sha):
        self.merges.append((number, expected_head_sha))
        return {"merged": True, "sha": "d" * 40}


class RestrictedMergeRunnerTest(unittest.TestCase):
    def test_unlabeled_owner_documentation_task_is_discovered_without_approval(self):
        client = FakeGitHub()
        client.pr["labels"] = []
        client.approvals = []
        client.files = [{"filename": "docs/tracker/2026-11-01-progress.md", "status": "added"}]
        self.assertEqual(run(REPO, "fake", apply=False, client=client),
                         [{"pr": 31, "status": "eligible"}])

    def test_dry_run_reports_candidate_but_never_writes(self):
        client = FakeGitHub()
        result = run(REPO, "fake", apply=False, client=client)
        self.assertEqual(result, [{"pr": 31, "status": "eligible"}])
        self.assertEqual(client.merges, [])

    def test_apply_merges_once_with_expected_head_sha(self):
        client = FakeGitHub()
        result = run(REPO, "fake", apply=True, client=client)
        self.assertEqual(result, [{"pr": 31, "status": "merged"}])
        self.assertEqual(client.merges, [(31, HEAD)])

    def test_changed_main_after_approval_blocks_last_second_merge(self):
        client = FakeGitHub(main_moves=True)
        result = run(REPO, "fake", apply=True, client=client)
        self.assertEqual(result, [{"pr": 31, "status": "stale-main"}])
        self.assertEqual(client.merges, [])

    def test_overlapping_pr_cannot_merge(self):
        client = FakeGitHub(conflicting=True)
        result = run(REPO, "fake", apply=True, client=client)
        self.assertEqual(result, [{"pr": 31, "status": "overlap"}])
        self.assertEqual(client.merges, [])

    def test_unauthorized_label_or_ci_failure_does_not_merge(self):
        for kind in ("label", "ci"):
            with self.subTest(kind=kind):
                client = FakeGitHub()
                if kind == "label":
                    client.pr["labels"] = []
                else:
                    client.runs[0]["conclusion"] = "failure"
                result = run(REPO, "fake", apply=True, client=client)
                self.assertFalse(any(item["status"] == "merged" for item in result))
                self.assertEqual(client.merges, [])


class RestrictedMergeWorkflowContractTest(unittest.TestCase):
    def test_apply_cli_requires_separate_app_token_not_default_github_token(self):
        with patch.dict("os.environ", {"GITHUB_TOKEN": "default-actions-token",
                                    "GITHUB_REPOSITORY": REPO}, clear=True):
            with patch("tools.tracker.restricted_merge.GitHubClient") as github_client:
                self.assertEqual(main(["--repository", REPO, "--apply"]), 2)
                github_client.assert_not_called()

    def test_workflow_is_explicitly_disabled_without_owner_configuration(self):
        workflow = (Path(__file__).resolve().parents[3] / ".github" / "workflows"
                    / "restricted-safe-automerge.yml").read_text(encoding="utf-8")
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_ENABLED", workflow)
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_CLIENT_ID", workflow)
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_PRIVATE_KEY", workflow)
        self.assertIn("actions/create-github-app-token@64ab729a6294000f5aa4a4bc0b73d1f32bc45510", workflow)
        self.assertIn("permission-contents: write", workflow)
        self.assertIn("permission-pull-requests: read", workflow)
        self.assertIn("PESATRACK_AUTOMERGE_APP_TOKEN", workflow)
        self.assertIn("persist-credentials: false", workflow)
        self.assertNotIn("GITHUB_TOKEN: ${{ github.token }}", workflow)
        self.assertNotIn("secrets.GITHUB_TOKEN", workflow)

    def test_unattended_lane_starts_when_app_is_configured_but_can_be_stopped(self):
        workflow = (Path(__file__).resolve().parents[3] / ".github" / "workflows"
                    / "restricted-safe-automerge.yml").read_text(encoding="utf-8")
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_ENABLED != 'false'", workflow)
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_CLIENT_ID != ''", workflow)
        self.assertIn("steps.credentials.outputs.configured == 'true'", workflow)
        self.assertIn("PESATRACK_SAFE_AUTOMERGE_PRIVATE_KEY", workflow)


if __name__ == "__main__":
    unittest.main()
