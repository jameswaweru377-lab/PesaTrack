"""Branch cleanup safeguards against invented GitHub REST responses."""

import json
from email.message import Message
from io import BytesIO
from types import SimpleNamespace
import unittest
from urllib.parse import unquote, urlparse
from urllib.error import HTTPError

from tools.tracker.branch_cleanup import (MAX_CANDIDATES_PER_RUN, _confirmed_ancestor,
                                          _recheck_candidate,
                                          _lease_delete, _verify_ref_absent,
                                          apply_plan, build_plan, collect_inventory)


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


class BulkRecheckOpener:
    def __init__(self, pulls):
        self.pulls = {row["number"]: row for row in pulls}

    def __call__(self, request, timeout=10):
        parsed = urlparse(request.full_url)
        if parsed.path.endswith("/repos/" + REPOSITORY):
            return Response({"full_name": REPOSITORY, "default_branch": "main"})
        if "/branches/" in parsed.path:
            name = unquote(parsed.path.split("/branches/", 1)[1])
            return Response(branch(name, FEATURE_SHA))
        if parsed.path.endswith("/pulls"):
            return Response([])
        if "/pulls/" in parsed.path:
            number = int(parsed.path.rsplit("/", 1)[1])
            return Response(self.pulls[number])
        if "/git/refs/heads/" in parsed.path and request.get_method() == "DELETE":
            return Response(status=204)
        raise AssertionError(f"Unexpected API request: {request.get_method()} {parsed.path}")


class ForbiddenOpener:
    def __call__(self, request, timeout=10):
        headers = Message()
        headers["X-GitHub-Request-Id"] = "REQ-SAFE-123"
        headers["X-RateLimit-Remaining"] = "0"
        headers["X-RateLimit-Reset"] = "1793532841"
        headers["X-Accepted-GitHub-Permissions"] = "contents=write, pull-requests=read"
        raise HTTPError(request.full_url, 403, "Forbidden", headers,
                        BytesIO(b"private response body"))


class BranchCleanupPlanTest(unittest.TestCase):
    def test_ancestor_recheck_fails_when_main_moves_after_preview(self):
        class MovingMainOpener:
            def __call__(self, request, timeout=30):
                path = urlparse(request.full_url).path
                if path.endswith("/pulls"):
                    return Response([])
                if path.endswith("/repos/" + REPOSITORY):
                    return Response({"full_name": REPOSITORY, "default_branch": "main"})
                if path.endswith("/branches/old%2Ffully-contained"):
                    return Response(branch("old/fully-contained", FEATURE_SHA))
                if path.endswith("/branches/main"):
                    return Response(branch("main", "d" * 40))
                raise AssertionError(path)

        candidate = {"branch": "old/fully-contained", "sha": FEATURE_SHA,
                     "default_branch": "main", "default_sha": MAIN_SHA,
                     "merged_pr": None, "evidence": "ancestor"}
        eligible, reason = _recheck_candidate(REPOSITORY, "", candidate,
                                              opener=MovingMainOpener())
        self.assertFalse(eligible)
        self.assertEqual("default_head_changed", reason)

    def test_apply_ancestor_mode_rechecks_before_single_lease_deletion(self):
        class StableOpener:
            def __call__(self, request, timeout=30):
                path = urlparse(request.full_url).path
                if path.endswith("/compare/" + FEATURE_SHA + "..." + MAIN_SHA):
                    return Response({"status": "ahead", "ahead_by": 4,
                                     "behind_by": 0,
                                     "base_commit": {"sha": FEATURE_SHA},
                                     "merge_base_commit": {"sha": FEATURE_SHA}})
                if path.endswith("/pulls"):
                    return Response([])
                if path.endswith("/repos/" + REPOSITORY):
                    return Response({"full_name": REPOSITORY, "default_branch": "main"})
                if path.endswith("/branches/old%2Ffully-contained"):
                    return Response(branch("old/fully-contained", FEATURE_SHA))
                if path.endswith("/branches/main"):
                    return Response(branch("main", MAIN_SHA))
                raise AssertionError(path)

        pushed = []
        snapshot = inventory([branch("main", MAIN_SHA),
                              branch("old/fully-contained", FEATURE_SHA)], [])
        result = apply_plan(REPOSITORY, "", snapshot, include_ancestors=True,
                            opener=StableOpener(),
                            pusher=lambda *args: pushed.append(args),
                            verifier=lambda *args, **kwargs: None)
        self.assertEqual(["old/fully-contained"], [row["branch"] for row in result["deleted"]])
        self.assertEqual([(REPOSITORY, "", "old/fully-contained", FEATURE_SHA)], pushed)

    def test_compare_proves_exact_ancestor_not_divergence_or_malformed_result(self):
        class CompareOpener:
            def __init__(self, result):
                self.result = result

            def __call__(self, request, timeout=30):
                self_url = request.full_url
                if "/compare/" not in self_url or FEATURE_SHA not in self_url or MAIN_SHA not in self_url:
                    raise AssertionError(self_url)
                return Response(self.result)

        valid = {"status": "ahead", "ahead_by": 12, "behind_by": 0,
                 "base_commit": {"sha": FEATURE_SHA},
                 "merge_base_commit": {"sha": FEATURE_SHA}}
        self.assertTrue(_confirmed_ancestor(REPOSITORY, "", FEATURE_SHA, MAIN_SHA,
                                            opener=CompareOpener(valid)))
        for changed in ({**valid, "behind_by": 1},
                        {**valid, "merge_base_commit": {"sha": "c" * 40}},
                        {**valid, "status": "diverged"},
                        {**valid, "base_commit": {}},
                        [valid]):
            self.assertFalse(_confirmed_ancestor(REPOSITORY, "", FEATURE_SHA, MAIN_SHA,
                                                 opener=CompareOpener(changed)))

    def test_ancestor_mode_adds_only_proven_behind_branch_without_changing_default(self):
        rows = [branch("main", MAIN_SHA), branch("old/fully-contained", FEATURE_SHA),
                branch("old/diverged", "c" * 40)]
        snapshot = inventory(rows, [])
        checked = []

        def ancestry(sha, main_sha):
            checked.append((sha, main_sha))
            return sha == FEATURE_SHA and main_sha == MAIN_SHA

        self.assertEqual([], build_plan(snapshot)["candidates"])
        result = build_plan(snapshot, include_ancestors=True, ancestry_checker=ancestry)
        self.assertEqual([{"branch": "old/fully-contained", "sha": FEATURE_SHA,
                           "merged_pr": None, "evidence": "ancestor",
                           "default_sha": MAIN_SHA}], result["candidates"])
        self.assertEqual([(FEATURE_SHA, MAIN_SHA), ("c" * 40, MAIN_SHA)], checked)

    def test_ancestor_mode_never_claims_open_or_protected_branch(self):
        rows = [branch("main", MAIN_SHA), branch("old/open", FEATURE_SHA),
                branch("old/protected", "c" * 40, protected=True)]
        snapshot = inventory(rows, [pull(77, "old/open", FEATURE_SHA,
                                         state="open", merged_at=None)])
        result = build_plan(snapshot, include_ancestors=True,
                            ancestry_checker=lambda *_: True)
        self.assertEqual([], result["candidates"])

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
        push_calls = []
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener,
                            pusher=lambda *args: push_calls.append(args),
                            verifier=lambda *a, **kw: None)
        self.assertEqual([{"branch": "feature/ready", "merged_pr": 42}], result["deleted"])
        self.assertEqual([(REPOSITORY, "test-token", "feature/ready", FEATURE_SHA)], push_calls)
        self.assertFalse(any(method == "DELETE" for method, _url, _timeout in opener.requests))

    def test_api_failure_stops_the_batch_without_deleting_candidate(self):
        opener = FakeOpener(fail_path="/pulls/42")
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener)
        self.assertEqual([], result["deleted"])
        self.assertEqual(1, len(result["failures"]))
        self.assertFalse(any(method == "DELETE" for method, _url, _timeout in opener.requests))

    def test_403_reports_safe_scope_and_rate_limit_diagnostics(self):
        result = apply_plan(REPOSITORY, "sensitive-token", inventory(), opener=ForbiddenOpener())
        self.assertEqual([], result["deleted"])
        reason = result["failures"][0]["reason"]
        self.assertIn("HTTP 403", reason)
        self.assertIn("remaining=0", reason)
        self.assertIn("reset=1793532841", reason)
        self.assertIn("accepted_permissions=contents=write, pull-requests=read", reason)
        self.assertNotIn("sensitive-token", reason)
        self.assertNotIn("private response body", reason)

    def test_apply_caps_candidates_to_stay_inside_the_token_request_budget(self):
        pulls = [pull(1000 + index, f"feature/candidate-{index:03}", FEATURE_SHA)
                 for index in range(MAX_CANDIDATES_PER_RUN + 1)]
        branches = [branch("main", MAIN_SHA)] + [
            branch(f"feature/candidate-{index:03}", FEATURE_SHA)
            for index in range(MAX_CANDIDATES_PER_RUN + 1)
        ]
        result = apply_plan(REPOSITORY, "test-token", inventory(branches, pulls),
                            opener=BulkRecheckOpener(pulls),
                            pusher=lambda *args: None,
                            verifier=lambda *args, **kwargs: None)
        self.assertEqual(MAX_CANDIDATES_PER_RUN + 1, result["candidate_count"])
        self.assertEqual(MAX_CANDIDATES_PER_RUN, result["processed_count"])
        self.assertEqual(1, result["deferred_count"])
        self.assertEqual(MAX_CANDIDATES_PER_RUN, len(result["deleted"]))

    def test_unknown_open_pr_head_repository_keeps_matching_branch(self):
        unknown = pull(99, "feature/ready", FEATURE_SHA, state="open", merged_at=None)
        unknown["head"]["repo"] = None
        planned = build_plan(inventory(pulls=[pull(42, "feature/ready", FEATURE_SHA), unknown]))
        self.assertEqual([], planned["candidates"])
        self.assertEqual(1, planned["skipped"]["open_pr"])
        opener = FakeOpener(open_pulls=[unknown])
        pushes = []
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener,
                            pusher=lambda *args: pushes.append(args))
        self.assertEqual([], pushes)
        self.assertEqual([], result["deleted"])
        self.assertEqual("open_pr_exists_or_repository_unknown", result["skipped"][0]["reason"])

    def test_remote_advance_after_last_get_blocks_lease_delete(self):
        opener = FakeOpener()
        attempts = []
        def simulated_race(repository, token, branch_name, expected):
            attempts.append((branch_name, expected))
            opener.branch_sha = "c" * 40
            raise RuntimeError("Exact-SHA Git lease deletion rejected or unavailable")
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener,
                            pusher=simulated_race)
        self.assertEqual([("feature/ready", FEATURE_SHA)], attempts)
        self.assertEqual("c" * 40, opener.branch_sha)
        self.assertEqual([], result["deleted"])
        self.assertEqual(1, len(result["failures"]))

    def test_lease_uses_one_ref_exact_sha_without_exposing_token(self):
        captured = []
        def mock_git(argv, **kwargs):
            captured.append((argv, kwargs))
            return SimpleNamespace(returncode=0)
        _lease_delete(REPOSITORY, "private-test-token", "feature/ready",
                      FEATURE_SHA, runner=mock_git)
        argv, kwargs = captured[0]
        self.assertEqual(1, len([x for x in argv if x.startswith("--force-with-lease=")]))
        self.assertIn(f"--force-with-lease=refs/heads/feature/ready:{FEATURE_SHA}", argv)
        self.assertEqual(":refs/heads/feature/ready", argv[-1])
        self.assertNotIn("private-test-token", str(argv))
        self.assertEqual("0", kwargs["env"]["GIT_TRACE"])
        self.assertTrue(kwargs["capture_output"])

    def test_failed_lease_never_claims_deletion_or_discloses_token(self):
        def reject(argv, **kwargs):
            return SimpleNamespace(returncode=1, stderr="private-test-token")
        with self.assertRaisesRegex(RuntimeError, "rejected") as error:
            _lease_delete(REPOSITORY, "private-test-token", "feature/ready",
                          FEATURE_SHA, runner=reject)
        self.assertNotIn("private-test-token", str(error.exception))

    def test_verification_failure_is_not_counted_as_deletion(self):
        opener = FakeOpener()
        def fails(*args, **kwargs):
            raise RuntimeError("not absent")
        result = apply_plan(REPOSITORY, "test-token", inventory(), opener=opener,
                            pusher=lambda *args: None, verifier=fails)
        self.assertEqual([], result["deleted"])
        self.assertEqual(1, len(result["failures"]))


    def test_deleted_ref_eventually_becomes_absent_without_repeating_push(self):
        calls = []
        pauses = []
        class StaleReads:
            def __call__(self, request, timeout=10):
                calls.append(request.get_method())
                if len(calls) <= 2:
                    return Response(branch("feature/ready", FEATURE_SHA))
                raise HTTPError(request.full_url, 404, "Not Found", Message(), None)
        _verify_ref_absent(REPOSITORY, "fake-token", "feature/ready", FEATURE_SHA,
                           opener=StaleReads(), sleeper=pauses.append)
        self.assertEqual(["GET"] * 3, calls)
        self.assertEqual([2, 2], pauses)

    def test_changed_ref_after_lease_is_not_accepted_as_stale_read(self):
        calls = []
        class ChangedRead:
            def __call__(self, request, timeout=10):
                calls.append(1)
                return Response(branch("feature/ready", "c" * 40))
        with self.assertRaisesRegex(RuntimeError, "changed or was recreated"):
            _verify_ref_absent(REPOSITORY, "fake-token", "feature/ready",
                               FEATURE_SHA, opener=ChangedRead(),
                               sleeper=lambda n: None)
        self.assertEqual(1, len(calls))

    def test_still_present_after_bounded_reads_fails_without_redelete(self):
        calls = []
        pauses = []
        class AlwaysStale:
            def __call__(self, request, timeout=10):
                calls.append(request.get_method())
                return Response(branch("feature/ready", FEATURE_SHA))
        with self.assertRaisesRegex(RuntimeError, "bounded"):
            _verify_ref_absent(REPOSITORY, "fake-token", "feature/ready",
                               FEATURE_SHA, opener=AlwaysStale(),
                               sleeper=pauses.append)
        self.assertEqual(["GET"] * 5, calls)
        self.assertEqual([2] * 4, pauses)

    def test_403_verification_fails_immediately_with_no_retry(self):
        calls = []
        class Forbidden:
            def __call__(self, request, timeout=10):
                calls.append(1)
                raise HTTPError(request.full_url, 403, "Forbidden", Message(), None)
        with self.assertRaisesRegex(RuntimeError, "HTTP 403"):
            _verify_ref_absent(REPOSITORY, "fake-token", "feature/ready",
                               FEATURE_SHA, opener=Forbidden(),
                               sleeper=lambda n: None)
        self.assertEqual(1, len(calls))


if __name__ == "__main__":
    unittest.main()
