"""Owner-approved one-time removal of retired-branch archive tags only.

The rolling release tag ci-apk is always preserved. Exact historical
namespace counts are required before any remote write is attempted.
"""

from __future__ import annotations

import argparse
import json
import subprocess


ARCHIVE_COUNTS = {
    "archive/retired-20261101/": 42,
    "archive/retired-20261101-wave2/": 42,
    "archive/retired-20261102-wave3/": 21,
}


def select_archive_tags(tags: list[str]) -> list[str]:
    if len(tags) != 106 or len(set(tags)) != 106 or tags.count("ci-apk") != 1:
        raise ValueError("Expected exactly 105 archive tags and one ci-apk tag")
    for prefix, count in ARCHIVE_COUNTS.items():
        if sum(name.startswith(prefix) for name in tags) != count:
            raise ValueError("Archive namespace count differs from owner-approved inventory")
    selected = [name for name in tags if name != "ci-apk"]
    if any(not any(name.startswith(p) for p in ARCHIVE_COUNTS) for name in selected):
        raise ValueError("Unknown tag in repository; stop without deleting")
    return selected


def remote_tags() -> list[str]:
    result = subprocess.run(["git", "ls-remote", "--refs", "--tags", "origin"],
                            text=True, capture_output=True, check=True)
    tags = []
    for line in result.stdout.splitlines():
        sha, ref = line.split("\t", 1)
        if len(sha) != 40 or not ref.startswith("refs/tags/"):
            raise ValueError("Unexpected remote tag listing")
        tags.append(ref.removeprefix("refs/tags/"))
    return tags


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    targets = select_archive_tags(remote_tags())
    print(json.dumps({"approved_archive_tags": len(targets),
                      "preserved": "ci-apk", "apply": args.apply}), flush=True)
    if args.apply:
        # Atomic deletion prevents a half-retired tag namespace on push failure.
        subprocess.run(["git", "push", "--atomic", "origin", "--delete", *targets],
                       check=True)
        remaining = remote_tags()
        if remaining != ["ci-apk"]:
            raise RuntimeError(f"Post-deletion tag inventory mismatch: {remaining!r}")
        print(json.dumps({"deleted": len(targets), "remaining": remaining}), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
