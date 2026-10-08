"""Read-only comparison of reviewed fixtures with an opt-in Kotlin test export.

No SMS/ledger importer, networking, model training, or financial writes. The CLI
has fixed repository paths; exports never supply the text used for inference.
"""
import hashlib
import json
import os
import re
from pathlib import Path

from tools.python_lab import financial_lab as lab

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = Path(__file__).parent / "fixtures" / "kotlin_corpus_manifest.json"
EXPORT = ROOT / "app" / "build" / "python-lab" / "kotlin-output.json"
CANONICAL_KINDS = frozenset({
    "PAYMENT", "RECEIPT", "TRANSFER", "CASH_WITHDRAWAL", "CASH_DEPOSIT",
    "UNRESOLVED_MOVEMENT", "REFUND", "REVERSAL", "DEBT_DRAWDOWN",
    "DEBT_REPAYMENT", "FEE", "BALANCE_OBSERVATION", "AVAILABLE_CREDIT_OBSERVATION",
})
OBSERVATIONS = {"BALANCE_OBSERVATION", "AVAILABLE_CREDIT_OBSERVATION"}


def validate_export(export, manifest, expected_sha):
    """Fail closed on stale, incomplete, unreviewed or expanded export data."""
    if not isinstance(expected_sha, str) or not re.fullmatch(r"[0-9a-f]{40}", expected_sha):
        raise ValueError("an explicit checkout SHA is required")
    if not isinstance(export, dict) or set(export) != {"schema_version", "corpus", "source_sha", "cases"}:
        raise ValueError("invalid export envelope")
    if type(export["schema_version"]) is not int or export["schema_version"] != 1:
        raise ValueError("unsupported schema")
    if export["source_sha"] != expected_sha or export["corpus"] != manifest["corpus"]:
        raise ValueError("stale export or wrong corpus")
    approved = {row["id"]: row for row in manifest["cases"]}
    if not approved or len(approved) != len(manifest["cases"]):
        raise ValueError("invalid reviewed manifest")
    if not isinstance(export["cases"], list):
        raise ValueError("cases must be a list")
    seen = set()
    rows = []
    for row in export["cases"]:
        if not isinstance(row, dict) or set(row) != {
            "id", "sha256", "kinds", "recognized", "requires_legacy_fallback"
        }:
            raise ValueError("unexpected export fields")
        item_id = row["id"]
        if not isinstance(item_id, str) or item_id not in approved or item_id in seen:
            raise ValueError("unreviewed or duplicate fixture")
        seen.add(item_id)
        if row["sha256"] != approved[item_id]["sha256"]:
            raise ValueError("fixture differs from reviewed sanitized text")
        kinds = row["kinds"]
        if not isinstance(kinds, list) or any(not isinstance(k, str) or k not in CANONICAL_KINDS for k in kinds):
            raise ValueError("unknown record kinds")
        if type(row["recognized"]) is not bool or type(row["requires_legacy_fallback"]) is not bool:
            raise ValueError("interpretation flags must be booleans")
        if not row["recognized"] and kinds:
            raise ValueError("unrecognized case cannot have canonical records")
        rows.append({**row, "text": approved[item_id]["text"]})
    if seen != set(approved):
        raise ValueError("missing reviewed fixtures")
    return rows


def expected_labels(kinds, fallback):
    """Compare only shared semantics; receipt is NOT proof of economic income.

    A transfer may be a savings transfer, but that subtype is not verified here.
    Debt semantics remain comparable even if persistence needs legacy fallback.
    Standalone observations carry no movement; reversal/refund provenance and
    cash movements cannot be established by the Python label-only baseline.
    """
    principal = [k for k in kinds if k not in OBSERVATIONS and k != "FEE"]
    if not principal:
        if "FEE" in kinds:
            return {"fee"}
        return None if fallback else {"no_movement"}
    if len(principal) != 1:
        return None
    return {
        "PAYMENT": {"expense"}, "TRANSFER": {"transfer", "savings_transfer"},
        "DEBT_DRAWDOWN": {"loan_drawdown"}, "DEBT_REPAYMENT": {"loan_repayment"},
        "REFUND": {"refund"},
    }.get(principal[0])


def compare(rows, predictions, source_sha):
    """Report label disagreements without inventing amounts or ledger events."""
    if len(rows) != len(predictions) or any(p is not None and p not in lab.KINDS for p in predictions):
        raise ValueError("one known label or abstention is required per fixture")
    counts = dict(total=len(rows), correct=0, incorrect=0, abstained=0, unsupported=0)
    unsafe = dict(non_income_as_income=0, false_positive_movement=0)
    cases = []
    for row, predicted in zip(rows, predictions):
        labels = expected_labels(row["kinds"], row["requires_legacy_fallback"])
        outcome = ("unsupported" if labels is None else "abstained" if predicted is None
                   else "correct" if predicted in labels else "incorrect")
        counts[outcome] += 1
        if labels is not None:
            unsafe["non_income_as_income"] += int(predicted == "income" and labels != {"no_movement"})
            unsafe["false_positive_movement"] += int(labels == {"no_movement"} and predicted not in {None, "no_movement"})
        cases.append({"id": row["id"], "kotlin_kinds": row["kinds"],
                      "expected_compatible_labels": sorted(labels) if labels is not None else None,
                      "predicted": predicted, "outcome": outcome,
                      "requires_legacy_fallback": row["requires_legacy_fallback"]})
    comparable = counts["total"] - counts["unsupported"]
    suggested = counts["correct"] + counts["incorrect"]
    return {"schema_version": 1, "source_sha": source_sha, "counts": counts,
            "coverage": round(suggested / comparable, 3) if comparable else None,
            "accuracy_when_suggested": round(counts["correct"] / suggested, 3) if suggested else None,
            "unsafe_suggestions": unsafe, "cases": cases,
            "candidate_gate": "blocked" if counts["incorrect"] else "not_assessed_for_integration",
            "production_connected": False,
            "limitations": ["Label compatibility only; no amount, balance, ownership or ledger correctness claim.",
                            "Receipts, cash movements, reversals and ambiguous cases are unscored.",
                            "Savings-transfer subtype and refund/debt chains are not verified.",
                            "Corpus is small, sanitized and not representative; no device or user-correction evaluation."]}


def _unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON field")
        result[key] = value
    return result


def run():
    """Fixed-file evaluation; a fresh export and explicit checkout SHA are required."""
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"), object_pairs_hook=_unique_object)
    for row in manifest["cases"]:
        digest = hashlib.sha256((row["title"] + "\n" + row["text"]).encode("utf-8")).hexdigest()
        if digest != row["sha256"]:
            raise ValueError("reviewed manifest hash mismatch")
    if EXPORT.is_symlink() or EXPORT.resolve().parent != EXPORT.parent.resolve() or EXPORT.stat().st_size > 100_000:
        raise ValueError("invalid export location or size")
    export = json.loads(EXPORT.read_text(encoding="utf-8"), object_pairs_hook=_unique_object)
    rows = validate_export(export, manifest, os.environ.get("PESATRACK_LAB_SOURCE_SHA"))
    training = lab.samples()
    if {r["text"] for r in rows} & {r["text"] for r in training}:
        raise ValueError("evaluation text overlaps training examples")
    return compare(rows, [lab.suggest_kind(r["text"], training)["suggestion"] for r in rows], export["source_sha"])


if __name__ == "__main__":
    import sys
    if len(sys.argv) != 1:
        raise SystemExit("No import arguments supported; reviewed fixtures only.")
    try:
        print(json.dumps(run(), indent=2, sort_keys=True))
    except (OSError, ValueError, KeyError, TypeError):
        raise SystemExit("Evaluation refused: missing, stale or invalid sanitized Kotlin export.")
