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
HUNK_START = re.compile(r"^@@ -\d+(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")
RAW_COLOR = re.compile(r"\bColor\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)")
RAW_RADIUS = re.compile(
    r"\b(?:RoundedCornerShape|CutCornerShape)\s*\([^)]*?\b\d+(?:\.\d+)?\.dp",
    re.DOTALL,
)


@dataclass(frozen=True)
class Violation:
    file: str
    line: int
    rule: str
    expression: str


def _scan_style_lines(filename: str, lines: list[tuple[int, str]]) -> list[Violation]:
    if not lines:
        return []
    source = "\n".join(
        "" if text.lstrip().startswith(("//", "*")) else text
        for _, text in lines
    )
    violations = []
    for rule, pattern in (("raw-color", RAW_COLOR), ("raw-corner-radius", RAW_RADIUS)):
        for match in pattern.finditer(source):
            line_index = source.count("\n", 0, match.start())
            violations.append(Violation(filename, lines[line_index][0], rule, match.group()))
    return violations


def scan_added_ui_lines(diff_text: str) -> list[Violation]:
    """Inspect a complete git diff; reject broken hunks instead of passing."""
    if not diff_text.strip():
        return []

    violations = []
    current_file = None
    next_line = None
    seen_diff = False
    saw_destination = False
    saw_hunk = False
    remaining_old = None
    remaining_new = None
    added_lines = []

    def flush_added_lines():
        nonlocal added_lines
        if current_file is not None:
            violations.extend(_scan_style_lines(current_file, added_lines))
        added_lines = []

    def finish_hunk():
        flush_added_lines()
        if remaining_old is not None and (remaining_old != 0 or remaining_new != 0):
            raise ValueError("Truncated diff hunk")

    def finish_file():
        finish_hunk()
        if saw_destination and not saw_hunk:
            raise ValueError("Diff file header has no hunk")

    for line in diff_text.splitlines():
        if line.startswith("diff --git "):
            if seen_diff:
                finish_file()
            seen_diff = True
            current_file = None
            next_line = None
            saw_destination = False
            saw_hunk = False
            remaining_old = None
            remaining_new = None
        elif line.startswith("+++ "):
            if not seen_diff:
                raise ValueError("Destination has no diff header")
            destination = line[4:]
            path = destination[2:] if destination.startswith("b/") else None
            current_file = path if path and UI_PATH.fullmatch(path) else None
            saw_destination = True
        elif line.startswith("--- ") or line.startswith(("index ", "new file mode ", "deleted file mode ", "similarity index ", "rename from ", "rename to ")):
            continue
        elif line.startswith("@@"):
            if not seen_diff or not saw_destination:
                raise ValueError("Hunk has no destination")
            finish_hunk()
            match = HUNK_START.match(line)
            if not match:
                raise ValueError("Invalid diff hunk header")
            remaining_old = int(match.group(1)) if match.group(1) is not None else 1
            next_line = int(match.group(2))
            remaining_new = int(match.group(3)) if match.group(3) is not None else 1
            saw_hunk = True
        elif line.startswith("+") and next_line is not None:
            if remaining_new <= 0:
                raise ValueError("Diff contains extra added lines")
            content = line[1:]
            added_lines.append((next_line, content))
            next_line += 1
            remaining_new -= 1
        elif line.startswith(" ") and next_line is not None:
            flush_added_lines()
            remaining_old -= 1
            remaining_new -= 1
            next_line += 1
        elif line.startswith("-") and next_line is not None:
            flush_added_lines()
            remaining_old -= 1
        elif line.startswith("\\ No newline"):
            continue
        elif line:
            raise ValueError("Unexpected diff line")

        if remaining_old is not None and (remaining_old < 0 or remaining_new < 0):
            raise ValueError("Diff hunk has more lines than declared")

    if not seen_diff:
        raise ValueError("Input is not a git diff")
    finish_file()
    return sorted(violations, key=lambda item: (item.file, item.line))


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
        source_lines = list(enumerate(path.read_text(encoding="utf-8").splitlines(), start=1))
        violations.extend(_scan_style_lines(filename, source_lines))
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
