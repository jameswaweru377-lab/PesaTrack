#!/usr/bin/env python3
"""Fast, offline PesaTrack security regression guard.

This is a narrow code-policy check, not malware scanning or a substitute for
SAST, dependency audits, on-device testing or a penetration test.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path
from xml.etree import ElementTree

ANDROID = "{http://schemas.android.com/apk/res/android}"
LOG_CALL = re.compile(r"(?<![A-Za-z_])(?:android\.util\.)?Log\.[diewv]\s*\(")
SECRET_PATTERNS = (
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    re.compile(r"\bghp_[A-Za-z0-9]{36}\b"),
)


def log_arguments(source: str, opening: int) -> int:
    """Count arguments of Log.* call, ignoring commas inside strings or calls."""
    depth = 1
    commas = 0
    quote = None
    triple = False
    i = opening + 1
    while i < len(source):
        char = source[i]
        if quote:
            if char == "\\":
                i += 2
                continue
            if triple and source.startswith('"""', i):
                quote, triple = None, False
                i += 3
                continue
            if not triple and char == quote:
                quote = None
            i += 1
            continue
        if source.startswith('"""', i):
            quote, triple = '"', True
            i += 3
            continue
        if char in ('"', "'"):
            quote, triple = char, False
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return commas + 1
        elif char == "," and depth == 1:
            commas += 1
        i += 1
    return 999  # invalid/incomplete call also fails closed


def inspect_logs(source: str, filename: str) -> list[str]:
    issues: list[str] = []
    for matched in LOG_CALL.finditer(source):
        count = log_arguments(source, matched.end() - 1)
        if count != 2:
            line = source.count("\n", 0, matched.start()) + 1
            issues.append(f"{filename}:{line}: Log call must have exactly two sanitized arguments")
    return issues


def scan_project(root: Path) -> list[str]:
    issues: list[str] = []
    sources = root / "app/src/main/java"
    manifest = root / "app/src/main/AndroidManifest.xml"
    if not sources.is_dir() or not manifest.is_file():
        return ["missing source tree or AndroidManifest.xml"]
    for file in sorted(sources.rglob("*.kt")):
        data = file.read_text(encoding="utf-8")
        issues.extend(inspect_logs(data, str(file.relative_to(root))))
        for pattern in SECRET_PATTERNS:
            if pattern.search(data):
                issues.append(f"{file.relative_to(root)}: looks like a committed private credential")

    try:
        app = ElementTree.parse(manifest).getroot().find("application")
    except ElementTree.ParseError:
        return issues + ["AndroidManifest.xml: invalid XML"]
    if app is None:
        return issues + ["AndroidManifest.xml: missing application"]
    if app.get(ANDROID + "allowBackup") != "false":
        issues.append("AndroidManifest.xml: automatic backup must stay disabled")
    if app.get(ANDROID + "usesCleartextTraffic") != "false":
        issues.append("AndroidManifest.xml: cleartext traffic must stay disabled")
    pdf = next((service for service in app.findall("service")
                if service.get(ANDROID + "name") == ".documents.PdfTextReviewService"), None)
    if pdf is None or pdf.get(ANDROID + "isolatedProcess") != "true" \
            or pdf.get(ANDROID + "exported") != "false":
        issues.append("AndroidManifest.xml: PDF extraction must remain isolated and unexported")
    for tag in ("receiver", "service", "provider"):
        for node in app.findall(tag):
            if node.get(ANDROID + "exported") != "false":
                issues.append("AndroidManifest.xml: unexpected exported " + tag)

    parser = sources / "com/kedevjimmy/pesatrack/documents/OfflineMpesaPdfReader.kt"
    if not parser.is_file():
        issues.append("offline statement parser is missing")
    else:
        code = parser.read_text(encoding="utf-8")
        admission = code.find("OfflinePdfAdmission.requireSupportedFile(data)")
        python = code.find('getModule("mpesa_pdf_engine")')
        if admission < 0 or python < 0 or admission >= python:
            issues.append("offline PDF header guard must run before the Python extractor")
    return issues


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
    errors = scan_project(root.resolve())
    if errors:
        print("PesaTrack security regression check FAILED:")
        for issue in errors:
            print(" - " + issue)
        return 1
    print("PesaTrack security regression check passed: sanitized Log calls, PDF guard, "
          "Android manifest and common private-key patterns.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
