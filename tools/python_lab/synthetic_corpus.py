"""Development-only monetary variations of the reviewed invented fixture corpus.

No arguments, SMS access, database access, network or model dependencies.
Only the fixed checked-in manifest is read; only the fixed build path is written.
"""
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
MONEY = re.compile(r"(\b(?:Ksh|KSh|KES)\s+)([0-9][0-9,]*(?:\.[0-9]{1,2})?)")


def generate():
    manifest = json.loads((ROOT / "tools/python_lab/fixtures/kotlin_corpus_manifest.json").read_text(encoding="utf-8"))
    cases = []
    for fixture in manifest["cases"]:
        digest = hashlib.sha256((fixture["title"] + "\n" + fixture["text"]).encode()).hexdigest()
        if digest != fixture["sha256"]:
            raise ValueError("Reviewed fixture hash mismatch")
        for scale in range(1, 129):
            def replace(match):
                amount = Decimal(match[2].replace(",", "")) * scale
                return match[1] + f"{amount:,.2f}"
            cases.append({
                "id": f"{fixture['id']}-{scale:03}",
                "fixture_id": fixture["id"], "scale": scale,
                "title": fixture["title"], "text": MONEY.sub(replace, fixture["text"]),
            })
    return {"schema_version": 1, "synthetic_only": True, "cases": cases}


def main():
    if len(sys.argv) != 1:
        raise SystemExit("No input arguments accepted; synthetic corpus only")
    destination = ROOT / "app/build/python-lab/synthetic-corpus.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(generate(), indent=2) + "\n", encoding="utf-8")
    print("Generated 2,432 invented monetary variants of 19 reviewed scenarios.")


if __name__ == "__main__":
    main()
