"""Superseded-PR cleanup cannot retire changed or unproven branches."""

import unittest

from tools.tracker.branch_cleanup import (_patch_lines_contained, _confirmed_superseded,
                                          _successor_files_unchanged, build_plan)
from tools.tracker.tests.test_branch_cleanup import REPOSITORY, MAIN_SHA, FEATURE_SHA, branch, inventory, pull
from tools.tracker.tests.test_branch_cleanup import Response
from urllib.parse import urlparse


class SupersededRetirementTests(unittest.TestCase):
    def test_patch_containment_requires_all_added_and_removed_lines(self):
        old = [{"filename": "a.kt", "patch": "@@ -1 +1 @@\n context\n-old\n+new", "additions": 1, "deletions": 1}]
        new = [{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n context\n-old\n+new\n+extra", "additions": 2, "deletions": 1}]
        self.assertTrue(_patch_lines_contained(old, new))
        new[0]["patch"] = "@@ -1 +1 @@\n-different\n+new"
        self.assertFalse(_patch_lines_contained(old, new))

    def test_patch_containment_fails_closed_on_missing_patch(self):
        self.assertFalse(_patch_lines_contained([{"filename": "a.kt"}], [{"filename": "a.kt", "patch": "+x"}]))
        self.assertFalse(_patch_lines_contained([], []))

    def test_truncated_nonempty_patch_fails_closed(self):
        truncated = [{"filename": "a.kt", "patch": "@@ -1 +1 @@\n-old\n+new",
                      "additions": 20, "deletions": 1}]
        successor = [{"filename": "a.kt", "patch": "@@ -1 +1 @@\n-old\n+new",
                      "additions": 1, "deletions": 1}]
        self.assertFalse(_patch_lines_contained(truncated, successor))

    def test_triple_prefix_content_is_not_mistaken_for_diff_header(self):
        old = [{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n+++unique\n+common",
                "additions": 2, "deletions": 0}]
        successor = [{"filename": "a.kt", "patch": "@@ -1 +1 @@\n+common",
                      "additions": 1, "deletions": 0}]
        self.assertFalse(_patch_lines_contained(old, successor))

    def test_repeated_changed_line_must_be_preserved_twice(self):
        old = [{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n+same\n+same",
                "additions": 2, "deletions": 0}]
        successor = [{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n+same\n+other",
                      "additions": 2, "deletions": 0}]
        self.assertFalse(_patch_lines_contained(old, successor))

    def test_same_changed_text_in_different_function_is_not_equivalent(self):
        old = [{"filename": "a.kt", "patch": "@@ -3 +3,2 @@\n fun debit() {\n+ validate()",
                "additions": 1, "deletions": 0}]
        successor = [{"filename": "a.kt", "patch": "@@ -8 +8,2 @@\n fun credit() {\n+ validate()",
                      "additions": 1, "deletions": 0}]
        self.assertFalse(_patch_lines_contained(old, successor))

    def test_explicit_superseded_pair_only_and_30_cap_independent(self):
        old = pull(200, "feature/old", FEATURE_SHA, merged_at=None)
        newer = pull(201, "feature/new", "c" * 40)
        snap = inventory([branch("main", MAIN_SHA), branch("feature/old", FEATURE_SHA)], [old, newer])
        pair = {"branch": "feature/old", "sha": FEATURE_SHA, "old_pr": 200, "successor_pr": 201}
        plan = build_plan(snap, superseded_pairs=[pair])
        self.assertEqual(["feature/old"], [item["branch"] for item in plan["candidates"]])
        self.assertEqual("superseded", plan["candidates"][0]["evidence"])
        self.assertEqual(30, plan["limit"])

    def test_changed_head_or_open_pr_not_retired(self):
        old = pull(200, "feature/old", FEATURE_SHA, merged_at=None)
        newer = pull(201, "feature/new", "c" * 40)
        pair = {"branch": "feature/old", "sha": FEATURE_SHA, "old_pr": 200, "successor_pr": 201}
        snap = inventory([branch("main", MAIN_SHA), branch("feature/old", "d" * 40)], [old, newer])
        self.assertEqual([], build_plan(snap, superseded_pairs=[pair])["candidates"])
        snap = inventory([branch("main", MAIN_SHA), branch("feature/old", FEATURE_SHA)], [old, newer,
                         pull(202, "feature/old", FEATURE_SHA, state="open", merged_at=None)])
        self.assertEqual([], build_plan(snap, superseded_pairs=[pair])["candidates"])

    def test_unlisted_merged_branches_are_not_selected(self):
        old = pull(200, "feature/old", FEATURE_SHA, merged_at=None)
        newer = pull(201, "feature/new", "c" * 40)
        other = pull(202, "feature/already-merged", "d" * 40)
        snap = inventory([branch("main", MAIN_SHA), branch("feature/old", FEATURE_SHA),
                          branch("feature/already-merged", "d" * 40)], [old, newer, other])
        pair = {"branch": "feature/old", "sha": FEATURE_SHA, "old_pr": 200, "successor_pr": 201}
        result = build_plan(snap, superseded_pairs=[pair])
        self.assertEqual(["feature/old"], [item["branch"] for item in result["candidates"]])

    def test_manifest_cannot_bypass_successor_proof_via_merged_pr(self):
        old = pull(200, "feature/old", FEATURE_SHA, merged_at=None)
        unrelated_merged = pull(300, "feature/old", FEATURE_SHA)
        successor = pull(201, "feature/new", "c" * 40)
        snap = inventory([branch("main", MAIN_SHA), branch("feature/old", FEATURE_SHA)],
                         [old, unrelated_merged, successor])
        pair = {"branch": "feature/old", "sha": FEATURE_SHA, "old_pr": 200, "successor_pr": 201}
        result = build_plan(snap, superseded_pairs=[pair])
        self.assertEqual("superseded", result["candidates"][0]["evidence"])

    def test_live_patch_proof_requires_merged_successor_and_complete_files(self):
        old = pull(200, "feature/old", FEATURE_SHA, merged_at=None)
        newer = pull(201, "feature/new", "c" * 40)
        candidate = {"branch": "feature/old", "sha": FEATURE_SHA,
                     "old_pr": 200, "successor_pr": 201, "default_branch": "main"}

        def opener(request, timeout=30):
            path = urlparse(request.full_url).path
            if path.endswith("/pulls/200"):
                return Response(old)
            if path.endswith("/pulls/201"):
                return Response(newer)
            if path.endswith("/pulls/200/files"):
                return Response([{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n context\n-old\n+new\n+extra", "additions": 2, "deletions": 1}])
            if path.endswith("/pulls/201/files"):
                return Response([{"filename": "a.kt", "patch": "@@ -1 +1,2 @@\n context\n-old\n+new\n+extra", "additions": 2, "deletions": 1}])
            raise AssertionError(path)

        self.assertTrue(_confirmed_superseded(REPOSITORY, "", candidate, opener=opener))
        newer["merged_at"] = None
        self.assertFalse(_confirmed_superseded(REPOSITORY, "", candidate, opener=opener))

    def test_successor_file_changes_or_missing_proof_block_retirement(self):
        sha = "e" * 40
        old_files = [{"filename": "a.kt", "patch": "@@ -1 +1 @@\n-old\n+new",
                      "additions": 1, "deletions": 1}]

        def same(request, timeout=30):
            path = urlparse(request.full_url).path
            if path.endswith("/pulls/200/files"):
                return Response(old_files)
            if path.endswith("/contents/a.kt"):
                return Response({"type": "file", "sha": sha})
            raise AssertionError(path)

        self.assertTrue(_successor_files_unchanged(REPOSITORY, "", 200,
                        "b" * 40, "c" * 40, MAIN_SHA, opener=same))

        def changed(request, timeout=30):
            result = same(request, timeout)
            if urlparse(request.full_url).path.endswith("/contents/a.kt"):
                value = {"type": "file", "sha": sha if "ref=" + "c" * 40 in request.full_url else "f" * 40}
                return Response(value)
            return result

        self.assertFalse(_successor_files_unchanged(REPOSITORY, "", 200,
                         "b" * 40, "c" * 40, MAIN_SHA, opener=changed))

        def conflict_resolution_dropped_change(request, timeout=30):
            path = urlparse(request.full_url).path
            if path.endswith("/pulls/200/files"):
                return Response(old_files)
            if path.endswith("/contents/a.kt"):
                blob = "f" * 40 if "ref=" + "b" * 40 in request.full_url else sha
                return Response({"type": "file", "sha": blob})
            raise AssertionError(path)

        self.assertFalse(_successor_files_unchanged(REPOSITORY, "", 200,
                         "b" * 40, "c" * 40, MAIN_SHA,
                         opener=conflict_resolution_dropped_change))


if __name__ == "__main__":
    unittest.main()
