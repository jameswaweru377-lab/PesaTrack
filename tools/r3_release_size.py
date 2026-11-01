"""Compare two unsigned APKs from the R3 release-shrinking experiment.

Checks archive integrity and native/Python packaging, not device behavior.
"""

import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def inspect_apk(path: Path) -> dict:
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError(f"Missing nonempty APK: {path.name}")
    with zipfile.ZipFile(path) as archive:
        damaged = archive.testzip()
        if damaged is not None:
            raise ValueError(f"Corrupt ZIP entry: {damaged}")
        names = archive.namelist()
    abis = sorted({name.split("/")[1] for name in names if name.startswith("lib/") and name.count("/") >= 2})
    python_assets = sorted(name for name in names if name.startswith("assets/chaquopy/") and not name.endswith("/"))
    if abis != ["arm64-v8a"]:
        raise ValueError(f"Unexpected packaged ABIs: {abis}")
    if not python_assets:
        raise ValueError("APK is missing Chaquopy Python assets")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return {
        "size_bytes": path.stat().st_size,
        "sha256": digest.hexdigest(),
        "abis": abis,
        "python_assets": python_assets,
    }


def compare(baseline: Path, minified: Path) -> dict:
    before, after = inspect_apk(baseline), inspect_apk(minified)
    if before["python_assets"] != after["python_assets"]:
        raise ValueError("Minification changed the bundled Chaquopy asset list")
    saved = before["size_bytes"] - after["size_bytes"]
    if saved <= 0:
        raise ValueError(f"Release APK did not shrink: delta {saved} bytes")
    return {
        "baseline_size_bytes": before["size_bytes"],
        "minified_size_bytes": after["size_bytes"],
        "saved_bytes": saved,
        "saved_percent": round(saved * 100 / before["size_bytes"], 2),
        "baseline_sha256": before["sha256"],
        "minified_sha256": after["sha256"],
        "abis": after["abis"],
        "chaquopy_asset_count": len(after["python_assets"]),
        "device_acceptance": "NOT RUN",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("minified", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = compare(args.baseline, args.minified)
    data = json.dumps(result, indent=2) + "\n"
    if args.output:
        args.output.write_text(data)
    print(data, end="")


if __name__ == "__main__":
    main()
