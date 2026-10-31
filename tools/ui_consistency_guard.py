#!/usr/bin/env python3
"""Report newly introduced one-off Compose colors and corner radii.

Runs on demand; it does not change CI, existing screens or design policy.
Only added Kotlin UI lines in a diff are checked. Existing styles are grandfathered.
"""

import argparse
from dataclasses import dataclass
from pathlib import Path
import re
import subprocess
import sys


UI_PATH = re.compile(r"^app/src/main/java/.+/ui/.+\.kt$")
HUNK_START = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")
RAW_COLOR = re.compile(r"\bColor\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)")
RAW_RADIUS = re.compile(
    r"\b(?:RoundedCornerShape|CutCornerShape)\s*\(\s*\d+(?:\.\d+)?\.dp\s*\)"
)


@dataclass(frozen=True)
class Violation:
    file: str
    line: int
    rule: str
    expression: str


def scan_added_ui_lines(diff_text: str) -> list[Violation]:
    """Inspect a git diff; legacy and removed lines are never violations."""
    if not diff_text.strip():
        return []

    violations = []
    current_file = None
    next_line = None
    seen_diff = False

    for line in diff_text.splitlines():
        if line.startswith("diff --git "):
            seen_diff = True
            current_file = None
            next_line = None
        elif line.startswith("+++ "):
            destination = line[4:]
            path = destination[2:] if destination.startswith("b/") else None
            current_file = path if path and UI_PATH.fullmatch(path) else None
        elif line.startswith("--- ") or line.startswith(("index ", "new file mode ", "deleted file mode ", "similarity index ", "rename from ", "rename to ")):
            continue
        elif line.startswith("@@"):
            match = HUNK_START.match(line)
            if not match:
                raise ValueError("Invalid diff hunk header")
            next_line = int(match.group(1))
        elif line.startswith("+") and next_line is not None:
            content = line[1:]
            if current_file is not None and not content.lstrip().startswith(("//", "*")):
                for rule, pattern in (("raw-color", RAW_COLOR), ("raw-corner-radius", RAW_RADIUS)):
                    for match in pattern.finditer(content):
                        violations.append(Violation(current_file, next_line, rule, match.group()))
            next_line += 1
        elif line.startswith(" ") and next_line is not None:
            next_line += 1
        elif line.startswith("-") or line.startswith("\\ No newline"):
            continue
        elif line and next_line is not None:
            raise ValueError("Unexpected diff line")

    if not seen_diff:
        raise ValueError("Input is not a git diff")
    return violations


def scan_untracked_ui_files() -> list[Violation]:
    """Include new files not yet staged, so a clean diff cannot hide them."""
    result = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard", "-z", "--", "app/src/main/java"],
        check=False, capture_output=True,
    )
    if result.returncode != 0:
        raise ValueError("Cannot list new UI files")

    violations = []
    for raw_path in result.stdout.split(b"\0"):
        if not raw_path:
            continue
        filename = raw_path.decode("utf-8", errors="surrogateescape")
        if not UI_PATH.fullmatch(filename):
            continue
        path = Path(filename)
        if path.is_symlink() or path.stat().st_size > 1_048_576:
            raise ValueError("Untracked UI file is not a regular bounded source file")
        for line_number, source_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if source_line.lstrip().startswith(("//", "*")):
                continue
            for rule, pattern in (("raw-color", RAW_COLOR), ("raw-corner-radius", RAW_RADIUS)):
                for match in pattern.finditer(source_line):
                    violations.append(Violation(filename, line_number, rule, match.group()))
    return violations


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="origin/main", help="Git revision to compare (default: origin/main)")
    parser.add_argument("--strict", action="store_true", help="Return exit code 1 if new styles are found")
    args = parser.parse_args(argv)

    command = ["git", "diff", "--no-ext-diff", "--unified=0", args.base, "--", "app/src/main/java"]
    try:
        result = subprocess.run(command, check=False, capture_output=True, text=True)
        if result.returncode != 0:
            raise ValueError(result.stderr.strip() or "git diff failed")
        violations = scan_added_ui_lines(result.stdout) + scan_untracked_ui_files()
    except (OSError, UnicodeError, ValueError) as error:
        print(f"Could not inspect UI changes: {error}", file=sys.stderr)
        return 2

    for violation in violations:
        print(f"{violation.file}:{violation.line}: {violation.rule}: {violation.expression}")
    if not violations:
        print("No newly introduced hardcoded UI colors or corner radii.")
    else:
        print(f"Found {len(violations)} newly introduced UI style violation(s).")
    return 1 if args.strict and violations else 0


if __name__ == "__main__":
    raise SystemExit(main())
