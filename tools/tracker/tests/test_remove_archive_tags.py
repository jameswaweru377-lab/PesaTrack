"""The owner-only removal must never touch the rolling release tag."""

import unittest

from tools.tracker.remove_archive_tags import select_archive_tags


class RemoveArchiveTagsTests(unittest.TestCase):
    def setUp(self):
        self.tags = ([f"archive/retired-20261101/item-{i}" for i in range(42)]
                     + [f"archive/retired-20261101-wave2/item-{i}" for i in range(42)]
                     + [f"archive/retired-20261102-wave3/item-{i}" for i in range(21)]
                     + ["ci-apk"])

    def test_select_exact_archive_tags_and_preserve_ci_apk(self):
        self.assertEqual(105, len(select_archive_tags(self.tags)))
        self.assertNotIn("ci-apk", select_archive_tags(self.tags))

    def test_unexpected_tags_fail_closed(self):
        for altered in ([x for x in self.tags if x != "ci-apk"],
                        self.tags + ["v1.0"],
                        self.tags[:-2] + ["ci-apk"],
                        self.tags + ["ci-apk"]):
            with self.subTest(altered_count=len(altered)):
                with self.assertRaises(ValueError):
                    select_archive_tags(altered)


if __name__ == "__main__":
    unittest.main()
