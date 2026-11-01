"""Branch cleanup safeguards against invented GitHub REST responses."""

import json
import unittest
from urllib.parse import urlparse

from tools.tracker.branch_cleanup import apply_plan, build_plan, collect_inventory


REPOSITORY = "jameswaweru377-lab/PesaTrack"
MAIN_SHA = "a" * 40
FEATURE_SHA = "b" * 40


def branch(name, sha, protected=False):
    return {"name": name, "commit": {"sha": sha}, "protected": protected}


def pull(number, name, sha, *, state="closed", merged_at="2026-10-01T12:00:00Z",
         base="main", head_repo=REPOSITORY, base_repo=REPOSITORY):
    return {
        "number": number,
        "state": state,
        "merged_at": merged_at,
        "head": {"ref": name, "sha": sha,
                 "repo": {"full_name": head_repo}},
        "base": {"ref": base, "repo": {"full_name": base_repo}},
    }


def inventory(branches=None, pulls=None, *, default_branch="main", complete=True):
    return {
        "repository": REPOSITORY,
        "default_branch": default_branch,
        "branches": list(branches if branches is not None else [branch("main", MAIN_SHA),
                                                                  branch("feature/ready", FEATURE_SHA)]),
        "pulls": list(pulls if pulls is not None else [pull(42, "feature/ready", FEATURE_SHA)]),
        "complete": complete,
    }


class Response:
    def __init__(self, value=None, status=200):
        self.value = value
        self.status = status

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self):
        return json.dumps(self.value).encode("utf-8") if self.value is not None else b""


class FakeOpener:
    def __init__(self, *, branch_sha=FEATURE_SHA, protected=False, default_branch="main",
                 current_pull=None, open_pulls=None, fail_path=None):
        self.branch_sha = branch_sha
        self.protected = protected
        self.default_branch = default_branch
        self.current_pull = current_pull or pull(42, "feature/ready", FEATURE_SHA)
        self.open_pulls = list(open_pulls or [])
        self.fail_path = fail_path
        self.requests = []

    def __call__(self, request, timeout=10):
        self.requests.append((request.get_method(), request.full_url, timeout))
        parsed = urlparse(request.full_url)
        if self.fail_path and parsed.path.endswith(self.fail_path):
            raise OSError("synthetic API outage")
        if parsed.path.endswith("/pulls/42"):
            return Response(self.current_pull)
        if parsed.path.endswith("/pulls"):
            return Response(self.open_pulls)
        if parsed.path.endswith("/branches/feature%2Fready") or parsed.path.endswith("/branches/feature/ready"):
            return Response(branch("feature/ready", self.branch_sha, self.protected))
        if parsed.path.endswith("/git/refs/heads/feature/ready") and request.get_method() == "DELETE":
            return Response(status=204)
        if parsed.path.endswith("/repos/" + REPOSITORY):
            return Response({"default_branch": self.default_branch})
        raise AssertionError(f"Unexpected API request: {request.get_method()} {parsed.path}")


class PaginationLimitOpener:
    def __call__(self, request, timeout=10):
        parsed = urlparse(request.full_url)
        if parsed.path.endswith("/repos/" + REPOSITORY):
            return Response({"full_name": REPOSITORY, "default_branch": "main"})
        if parsed.path.endswith("/branches"):
            return Response([branch(f"feature/{index}", FEATURE_SHA) for index in range(100)])
        raise AssertionError(f"Unexpected API request: {parsed.path}")


class BranchCleanupPlanTest(unittest.TestCase):
    def test_only_exact_merged_head_into_default_branch_is_a_candidate(self):
        rows = [
            pull(42, "feature/ready", FEATURE_SHA),
            pull(43, "feature/wrong-sha", "c" * 40),
            pull(44, "feature/non-main", "d" * 40, base="release"),
            pull(45, "feature/foreign", "e" * 40, head_repo="fork/PesaTrack"),
            pull(46, "feature/foreign-base", "f" * 40, base_repo="fork/PesaTrack"),
            pull(47, "feature/unmerged", "1" * 40, merged_at=None),
            pull(48, "feature/open", "2" * 40, state="open", merged_at=None),
        ]
        branches = [
            branch("main", MAIN_SHA),
            branch("feature/ready", FEATURE_SHA),
            branch("feature/wrong-sha", "3" * 40),
            branch("feature/non-main", "d" * 40),
            branch("feature/foreign", "e" * 40),
            branch("feature/foreign-base", "f" * 40),
            branch("feature/unmerged", "1" * 40),
            branch("feature/open", "2" * 40),
            branch("feature/protected", "4" * 40, protected=True),
            branch("feature/no-pr", "5" * 40),
        ]
        rows.append(pull(49, "feature/protected", "4" * 40))

        result = build_plan(inventory(branches, rows))

        self.assertEqual([{"branch": "feature/ready", "sha": FEATURE_SHA,
                           "merged_pr": 42}], result["candidates"])

    def test_incomplete_or_malformed_inventory_fails_closed(self):
        with self.assertRaises((RuntimeError, ValueError)):
            build_plan(inventory(complete=False))
        malformed = inventory()
        malformed["branches"] = [{"name": "feature/ready", "commit": {}, "protected": False}]
        with self.assertRaises((RuntimeError, ValueError)):
            build_plan(malformed)

    def test_pagination_at_the_page_limit_fails_closed(self):
        with self.assertRaisesRegex(RuntimeError, "safety limit"):
            collect_inventory(REPOSITORY, "test-token", opener=PaginationLimitOpener(), max_pages=1)

    def test_every_excluded_branch_state_produces_no_delete_request(self):
        cases = (
            (branch("feature/candidate", "c" * 40),
             [pull(42, "feature/candidate", FEATURE_SHA)], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA),
             [pull(42, "feature/candidate", FEATURE_SHA, base="release")], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA),
             [pull(42, "feature/candidate", FEATURE_SHA, head_repo="fork/PesaTrack")], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA),
             [pull(42, "feature/candidate", FEATURE_SHA, base_repo="fork/PesaTrack")], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA),
             [pull(42, "feature/candidate", FEATURE_SHA, merged_at=None)], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA, protected=True),
             [pull(42, "feature/candidate", FEATURE_SHA)], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA), [], FakeOpener()),
            (branch("feature/candidate", FEATURE_SHA),
             [pull(42, "feature/candidate", FEATURE_SHA, state="open", merged_at=None)], FakeOpener()),
            (branch("main", MAIN_SHA), [pull(42, "main", MAIN_SHA)], FakeOpener()),
        )
        for branch_row, pulls, opener in cases:
            with self.subTest(branch=branch_row["name"], pulls=pulls):
                branch_rows = ([branch_row] if branch_row["name"] == "main"
                               else [branch("main", MAIN_SHA), branch_row])
                snapshot = inventory(branch_rows, pulls)
                result = apply_plan(REPOSITORY, "test-token", snapshot, opener=opener)
                self.assertEqual([], result["deleted"])
                self.assertFalse(any(method == "DELETE" for method, _url, _timeout in opener.requests))

    def test_new_open_pr_or_changed_branch_tip_prevents_deletion(self):
        for opener in (
            FakeOpener(open_pulls=[pull(99, "feature/ready", FEATURE_SHA,
                                        state="open", merged_at=None)]),
            FakeOpener(branch_sha="c" * 40),
            FakeOpener(protected=True),
            FakeOpener(default_branch="trunk"),
        ):
            result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener)
            self.assertEqual([], result["deleted"])
            self.assertTrue(any(method == "GET" for method, _url, _timeout in opener.requests))
            self.assertFalse(any(method == "DELETE" for method, _url, _timeout in opener.requests))

    def test_slash_branch_is_deleted_only_after_full_recheck(self):
        opener = FakeOpener()
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener)
        deletes = [url for method, url, _timeout in opener.requests if method == "DELETE"]
        self.assertEqual(["feature/ready"], result["deleted"])
        self.assertEqual(1, len(deletes))
        self.assertTrue(urlparse(deletes[0]).path.endswith("/git/refs/heads/feature/ready"))

    def test_api_failure_skips_candidate_without_delete(self):
        opener = FakeOpener(fail_path="/pulls/42")
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener)
        self.assertEqual([], result["deleted"])
        self.assertFalse(any(method == "DELETE" for method, _url, _timeout in opener.requests))


if __name__ == "__main__":
    unittest.main()
