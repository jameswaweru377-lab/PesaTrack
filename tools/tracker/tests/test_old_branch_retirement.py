"""User-approved aged branch retirement requires exact tips and recovery tags."""

import unittest

from tools.tracker.old_branch_retirement import plan_retirement, apply_retirement, _archive_tip
from tools.tracker.tests.test_branch_cleanup import (
    REPOSITORY, MAIN_SHA, FEATURE_SHA, FakeOpener, Response, branch, inventory, pull,
)
from urllib.parse import urlparse
from urllib.error import HTTPError
import json


def manifest(sha=FEATURE_SHA):
    return {"repository": REPOSITORY, "approved_issue": 528,
            "cutoff_tip_date": "2026-10-02", "archive_prefix": "archive/retired-20261101",
            "candidates": ([{"branch": "feature/ready", "sha": sha, "tip_date": "2026-09-29"}]
                           + [{"branch": f"feature/retired-{i}", "sha": FEATURE_SHA,
                               "tip_date": "2026-09-29"} for i in range(41)])}


class OldBranchRetirementTests(unittest.TestCase):
    def test_explicit_approved_tip_is_candidate(self):
        result = plan_retirement(inventory(), manifest())
        self.assertEqual(["feature/ready"], [x["branch"] for x in result["candidates"]])
        self.assertEqual("approved_age", result["candidates"][0]["evidence"])

    def test_second_batch_uses_independent_recovery_namespace(self):
        data = manifest()
        data["archive_prefix"] = "archive/retired-20261101-wave2"
        data["cutoff_tip_date"] = "2026-10-09"
        data["candidates"][0]["tip_date"] = "2026-10-09"
        result = plan_retirement(inventory(), data)
        self.assertEqual("archive/retired-20261101-wave2/feature/ready",
                         result["candidates"][0]["archive_ref"])
        data["cutoff_tip_date"] = "2026-10-10"
        with self.assertRaises(ValueError):
            plan_retirement(inventory(), data)
        data["cutoff_tip_date"] = "2026-10-09"
        data["archive_prefix"] = "archive/retired-unapproved"
        with self.assertRaises(ValueError):
            plan_retirement(inventory(), data)

    def test_changed_open_and_protected_branches_are_not_selected(self):
        self.assertEqual([], plan_retirement(inventory(), manifest("c" * 40))["candidates"])
        snap = inventory([branch("main", MAIN_SHA), branch("feature/ready", FEATURE_SHA, True)])
        self.assertEqual([], plan_retirement(snap, manifest())["candidates"])
        snap = inventory(pulls=[pull(42, "feature/ready", FEATURE_SHA, state="open", merged_at=None)])
        self.assertEqual([], plan_retirement(snap, manifest())["candidates"])

    def test_invalid_or_recent_manifest_fails_closed(self):
        data = manifest()
        data["candidates"][0]["tip_date"] = "2026-10-26"
        with self.assertRaises(ValueError):
            plan_retirement(inventory(), data)
        data = manifest()
        data["candidates"] = data["candidates"][:1]
        with self.assertRaises(ValueError):
            plan_retirement(inventory(), data)
        data = manifest()
        data["candidates"] *= 43
        with self.assertRaises(ValueError):
            plan_retirement(inventory(), data)

    def test_archive_completes_before_delete(self):
        actions = []
        result = apply_retirement(REPOSITORY, "token", inventory(), manifest(),
            opener=FakeOpener(),
            archiver=lambda *args: actions.append("archive"),
            pusher=lambda *args: actions.append("delete"),
            verifier=lambda *args, **kwargs: actions.append("verify"))
        self.assertEqual(["archive", "delete", "verify"], actions)
        self.assertEqual(1, len(result["deleted"]))

    def test_archive_failure_never_deletes(self):
        actions = []
        def fail(*_args):
            raise RuntimeError("archive unavailable")
        result = apply_retirement(REPOSITORY, "token", inventory(), manifest(),
            opener=FakeOpener(), archiver=fail,
            pusher=lambda *args: actions.append("delete"))
        self.assertEqual([], actions)
        self.assertEqual([], result["deleted"])
        self.assertEqual(1, len(result["failures"]))

    def test_archive_creates_and_verifies_exact_sha_via_github(self):
        seen = []
        archive = "archive/retired-20261101/feature/ready"

        def opener(req, timeout=30):
            seen.append((req.get_method(), urlparse(req.full_url).path))
            if req.get_method() == "GET" and len(seen) == 1:
                raise HTTPError(req.full_url, 404, "not found", {}, None)
            if req.get_method() == "POST":
                payload = json.loads(req.data.decode("utf-8"))
                self.assertEqual({"ref": "refs/tags/" + archive, "sha": FEATURE_SHA}, payload)
                return Response({"ref": "refs/tags/" + archive})
            return Response({"ref": "refs/tags/" + archive,
                             "object": {"type": "commit", "sha": FEATURE_SHA}})

        _archive_tip(REPOSITORY, "token", "feature/ready", FEATURE_SHA, archive, opener=opener)
        self.assertEqual(["GET", "POST", "GET"], [m for m, _ in seen])

    def test_existing_matching_archive_is_reusable_without_a_second_post(self):
        methods = []
        archive = "archive/retired-20261101/feature/ready"
        def opener(req, timeout=30):
            methods.append(req.get_method())
            return Response({"ref": "refs/tags/" + archive,
                             "object": {"type": "commit", "sha": FEATURE_SHA}})
        _archive_tip(REPOSITORY, "token", "feature/ready", FEATURE_SHA, archive, opener=opener)
        self.assertEqual(["GET"], methods)


if __name__ == "__main__":
    unittest.main()
