"""GitHub adapter contracts against invented REST responses, with no network."""

import json
import unittest
from urllib.parse import parse_qs, urlparse

from tools.tracker.github_snapshot import fetch_snapshot


class FakeGitHub:
    def __init__(self):
        self.requested = []

    def __call__(self, url, headers, timeout):
        self.requested.append((url, headers.copy(), timeout))
        parsed = urlparse(url)
        path = parsed.path
        query = parse_qs(parsed.query)
        page = int(query.get("page", ["1"])[0])
        if path.endswith("/commits/main"):
            return {"sha": "b" * 40}
        if path.endswith("/issues"):
            return ([{"number": 123, "title": "Invented issue", "state": "open",
                      "body": "Chat owner: CHAT-STATEMENTS", "labels": []}]
                    if page == 1 else [])
        if path.endswith("/pulls"):
            return ([{"number": 201, "state": "open", "body": "Tracks #123\nChat owner: CHAT-STATEMENTS",
                      "head": {"sha": "a" * 40}, "base": {"sha": "b" * 40},
                      "merged_at": None, "merge_commit_sha": None}]
                    if page == 1 else [])
        if path.endswith("/pulls/201/files"):
            return [{"filename": "app/src/Invented.kt"}]
        if path.endswith("/actions/runs"):
            self.assert_url_head_sha(query)
            return {"workflow_runs": [{"name": "Android CI", "head_sha": "a" * 40,
                                       "status": "completed", "conclusion": "success"}]}
        raise AssertionError(f"Unexpected REST path: {path}")

    @staticmethod
    def assert_url_head_sha(query):
        assert query.get("head_sha") == ["a" * 40]


class GitHubSnapshotTest(unittest.TestCase):
    def test_fetches_current_main_issue_pr_paths_and_current_head_checks(self):
        fake = FakeGitHub()
        result = fetch_snapshot("invented-token", "test-org/test-repo", opener=fake)
        self.assertEqual("b" * 40, result["main_sha"])
        self.assertEqual([123], [i["number"] for i in result["issues"]])
        self.assertEqual(["app/src/Invented.kt"], result["prs"][0]["files"])
        self.assertEqual("success", result["checks"][0]["conclusion"])
        self.assertTrue(all(url.startswith("https://api.github.com/repos/test-org/test-repo/")
                            for url, _, _ in fake.requested))
        self.assertTrue(all(h["Authorization"] == "Bearer invented-token" for _, h, _ in fake.requested))
        self.assertTrue(all(timeout <= 12 for _, _, timeout in fake.requested))

    def test_pr_opt_in_label_is_normalized_without_assuming_automerge_approval(self):
        class Labeled(FakeGitHub):
            def __call__(self, url, headers, timeout):
                result = super().__call__(url, headers, timeout)
                if urlparse(url).path.endswith("/pulls"):
                    result[0]["labels"] = [{"name": "pesatrack-safe-automerge"}]
                return result

        result = fetch_snapshot("", "test-org/test-repo", opener=Labeled())
        self.assertEqual(["pesatrack-safe-automerge"], result["prs"][0]["labels"])

    def test_open_prs_are_fetched_independently_of_large_closed_pr_history(self):
        class Historic(FakeGitHub):
            def __call__(self, url, headers, timeout):
                parsed = urlparse(url)
                if parsed.path.endswith("/pulls"):
                    state = parse_qs(parsed.query).get("state", ["all"])[0]
                    if state == "open":
                        return [{"number": 301, "state": "open", "body": "Unlinked change",
                                 "head": {"sha": "a" * 40}, "base": {"sha": "b" * 40}}]
                    if state == "closed":
                        return []
                    return [{"number": n, "state": "closed", "body": "Historical",
                             "head": {"sha": "a" * 40}, "base": {"sha": "b" * 40}}
                            for n in range(100)]
                if parsed.path.endswith("/pulls/301/files"):
                    return [{"filename": "app/src/Separate.kt"}]
                return super().__call__(url, headers, timeout)

        result = fetch_snapshot("", "test-org/test-repo", opener=Historic(), max_pages=2)
        self.assertIn(301, [pull["number"] for pull in result["prs"]])

    def test_http_pagination_is_bounded_and_does_not_fetch_every_old_pr_file(self):
        class ManyPages(FakeGitHub):
            def __call__(self, url, headers, timeout):
                parsed = urlparse(url)
                if parsed.path.endswith("/issues"):
                    self.requested.append((url, headers, timeout))
                    return [{"number": n + 1, "title": "Synthetic", "state": "open",
                             "body": "Chat owner: Unclaimed"} for n in range(100)]
                if parsed.path.endswith("/pulls"):
                    self.requested.append((url, headers, timeout))
                    return []
                return super().__call__(url, headers, timeout)

        fake = ManyPages()
        result = fetch_snapshot("", "test-org/test-repo", opener=fake, max_pages=2)
        self.assertEqual(200, len(result["issues"]))
        self.assertEqual(2, sum("/issues?" in url for url, _, _ in fake.requested))
        self.assertFalse(any("Authorization" in headers for _, headers, _ in fake.requested))

    def test_invalid_repository_does_not_become_http_request(self):
        for repository in ("", "https://evil.com/org/repo", "../../etc/passwd", "one-name"):
            with self.subTest(repository=repository):
                with self.assertRaises(ValueError):
                    fetch_snapshot("secret", repository, opener=FakeGitHub())

    def test_workflow_run_from_different_sha_cannot_claim_ci_passed(self):
        class Stale(FakeGitHub):
            def __call__(self, url, headers, timeout):
                data = super().__call__(url, headers, timeout)
                if urlparse(url).path.endswith("/actions/runs"):
                    data["workflow_runs"][0]["head_sha"] = "z" * 40
                return data

        result = fetch_snapshot("invented", "test-org/test-repo", opener=Stale())
        self.assertEqual([], result["checks"])

    def test_existing_issue_pr_link_includes_historical_merged_pr_without_tracks_line(self):
        class Historical(FakeGitHub):
            def __call__(self, url, headers, timeout):
                path = urlparse(url).path
                if path.endswith("/issues"):
                    self.requested.append((url, headers.copy(), timeout))
                    return [{"number": 123, "title": "Historic", "state": "open",
                             "body": "Source: R4\nPR: #478", "labels": [{"name": "pesatrack-tracker"}]}]
                if path.endswith("/pulls"):
                    self.requested.append((url, headers.copy(), timeout))
                    return [{"number": 478, "state": "closed", "body": "Old proposal",
                             "head": {"sha": "a" * 40}, "base": {"sha": "b" * 40},
                             "merged_at": "2026-10-31T09:00:00Z", "merge_commit_sha": "c" * 40}]
                if path.endswith("/pulls/478/files"):
                    self.requested.append((url, headers.copy(), timeout))
                    return [{"filename": "app/src/Invented.kt"}]
                return super().__call__(url, headers, timeout)

        snapshot = fetch_snapshot("invented", "test-org/test-repo", opener=Historical())
        self.assertEqual([478], [p["number"] for p in snapshot["prs"]])

    def test_explicit_historical_pr_outside_recent_pages_is_fetched_by_number(self):
        class OldPull(FakeGitHub):
            def __call__(self, url, headers, timeout):
                path = urlparse(url).path
                if path.endswith("/issues"):
                    self.requested.append((url, headers.copy(), timeout))
                    return [{"number": 123, "title": "Historic", "state": "open",
                             "body": "PR: #478"}]
                if path.endswith("/pulls"):
                    self.requested.append((url, headers.copy(), timeout))
                    return []
                if path.endswith("/pulls/478"):
                    self.requested.append((url, headers.copy(), timeout))
                    return {"number": 478, "state": "closed", "body": "Old approved work",
                            "head": {"sha": "a" * 40}, "base": {"sha": "b" * 40},
                            "merged_at": "2026-10-30T09:00:00Z", "merge_commit_sha": "c" * 40}
                if path.endswith("/pulls/478/files"):
                    self.requested.append((url, headers.copy(), timeout))
                    return [{"filename": "app/src/Old.kt"}]
                return super().__call__(url, headers, timeout)

        snapshot = fetch_snapshot("invented", "test-org/test-repo", opener=OldPull())
        self.assertEqual([478], [p["number"] for p in snapshot["prs"]])
        self.assertEqual("2026-10-30T09:00:00Z", snapshot["prs"][0]["merged_at"])


if __name__ == "__main__":
    unittest.main()
