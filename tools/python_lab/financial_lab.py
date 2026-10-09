"""Offline, non-authoritative financial intelligence experiments for PesaTrack.

Synthetic fixtures only. No app DB, SMS reader, HTTP, third-party packages or
production inference path. Tests in this directory enforce the boundary.
"""

from __future__ import annotations

import json
import math
import re
from collections import Counter, defaultdict
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path
from statistics import median

FIXTURES = Path(__file__).parent / "fixtures" / "synthetic_cases.json"
KINDS = frozenset({
    "income", "expense", "transfer", "savings_transfer", "loan_drawdown",
    "loan_repayment", "fee", "refund", "no_movement",
})
_SPEND = frozenset({"expense", "fee"})
_TOKEN = re.compile(r"[a-z]+")
_IGNORED = frozenset({"kes", "ksh", "kshs", "the", "your", "you", "to", "of", "a", "an", "is", "was", "for", "on", "at"})


def samples() -> list[dict]:
    """Load the repository's explicitly fake, developer-authored examples."""
    return json.loads(FIXTURES.read_text(encoding="utf-8"))["messages"]


def tokenize(message: str) -> Counter[str]:
    """Lightweight, provider-neutral language features; NOT accounting rules."""
    words = [w for w in _TOKEN.findall(message.lower()) if w not in _IGNORED]
    words = [w for w in words if len(w) > 2]
    # Adjacent word pairs capture meaning that a bag of words alone misses.
    return Counter(words + ["_".join(pair) for pair in zip(words, words[1:])])


def similarity(a: Counter[str], b: Counter[str]) -> float:
    common = a.keys() & b.keys()
    dot = sum(a[k] * b[k] for k in common)
    norm = math.sqrt(sum(v*v for v in a.values()) * sum(v*v for v in b.values()))
    return dot / norm if norm else 0.0


def suggest_kind(text: str, labelled: list[dict], *, min_similarity: float = 0.28, min_margin: float = 0.06) -> dict:
    """Suggest a synthetic label or abstain; never authorize a ledger write.

    This is a transparent nearest-example baseline, NOT a trained production AI.
    Scores are similarity measurements, not calibrated probabilities.
    """
    if not 0 <= min_similarity <= 1 or not 0 <= min_margin <= 1:
        raise ValueError("thresholds must be between 0 and 1")
    query = tokenize(text)
    scores: dict[str, float] = defaultdict(float)
    for item in labelled:
        kind = item["kind"]
        if kind not in KINDS:
            raise ValueError(f"unknown training label: {kind}")
        scores[kind] = max(scores[kind], similarity(query, tokenize(item["text"])))
    ordered = sorted(scores.items(), key=lambda pair: (-pair[1], pair[0]))
    if not ordered:
        return {"suggestion": None, "similarity": 0.0, "reason": "no_examples"}
    kind, top = ordered[0]
    runner_up = ordered[1][1] if len(ordered) > 1 else 0.0
    if top < min_similarity or top - runner_up < min_margin:
        return {"suggestion": None, "similarity": round(top, 3), "reason": "uncertain"}
    return {"suggestion": kind, "similarity": round(top, 3), "reason": "experimental_only"}


def evaluate(messages: list[dict]) -> dict:
    """Leave-one-out assessment; count incorrect predictions and abstentions."""
    if len(messages) < 2:
        raise ValueError("need at least two labelled examples")
    correct = wrong = abstained = 0
    failures = []
    for index, example in enumerate(messages):
        other = messages[:index] + messages[index+1:]
        prediction = suggest_kind(example["text"], other)
        predicted = prediction["suggestion"]
        if predicted is None:
            abstained += 1
        elif predicted == example["kind"]:
            correct += 1
        else:
            wrong += 1
        if predicted != example["kind"]:
            failures.append({"id": example["id"], "expected": example["kind"], "predicted": predicted})
    total = len(messages)
    return {
        "total": total, "correct": correct, "incorrect": wrong,
        "abstained": abstained, "coverage": round((correct+wrong)/total, 3),
        "accuracy_when_suggested": round(correct/(correct+wrong), 3) if correct+wrong else None,
        "failures": failures,
    }


def _amount(value: object) -> Decimal:
    """Reject floats, zero and negative principal; preserve exact cents."""
    if not isinstance(value, str):
        raise ValueError("amount must be a decimal string")
    try:
        number = Decimal(value)
    except InvalidOperation as exc:
        raise ValueError("invalid amount") from exc
    if not number.is_finite() or number <= 0 or number.as_tuple().exponent < -2:
        raise ValueError("amount must be positive with at most two decimal places")
    return number


def validate_ledger(events: list[dict]) -> list[str]:
    """Check synthetic *economic events*; never repair or infer missing data."""
    errors: list[str] = []
    ids: set[str] = set()
    for row in events:
        item_id = str(row.get("id", ""))
        if not item_id or item_id in ids:
            errors.append(f"duplicate_or_missing_id:{item_id}")
        ids.add(item_id)
        kind = row.get("kind")
        if kind not in KINDS:
            errors.append(f"unknown_kind:{item_id}")
        try:
            date.fromisoformat(row["date"])
        except (ValueError, TypeError, KeyError):
            errors.append(f"invalid_date:{item_id}")
        try:
            _amount(row["amount"])
        except (ValueError, KeyError):
            errors.append(f"invalid_amount:{item_id}")
        if kind == "refund" and not row.get("linked_event_id"):
            errors.append(f"refund_requires_link:{item_id}")
        if kind in {"transfer", "savings_transfer"} and not row.get("destination"):
            errors.append(f"transfer_requires_destination:{item_id}")
        if kind == "no_movement":
            errors.append(f"no_movement_not_ledger_event:{item_id}")
    for row in events:
        if row.get("kind") == "refund" and row.get("linked_event_id") not in ids:
            errors.append(f"refund_unknown_link:{row.get('id')}")
    return errors


def monthly_summary(events: list[dict], month: str) -> dict:
    """Summarize canonical synthetic records, never raw dual-channel messages.

    Loan principal, transfers and savings movements are deliberately excluded
    from income/spending. Debt repayments remain a distinct cash obligation.
    """
    errors = validate_ledger(events)
    if errors:
        raise ValueError("invalid synthetic ledger: " + ", ".join(errors))
    if not re.fullmatch(r"\d{4}-\d{2}", month):
        raise ValueError("month must be YYYY-MM")
    totals = defaultdict(Decimal)
    categories = defaultdict(Decimal)
    for item in events:
        if item["date"][:7] != month:
            continue
        kind = item["kind"]
        amount = _amount(item["amount"])
        totals[kind] += amount
        if kind in _SPEND:
            categories[item.get("category") or "Uncategorized"] += amount
        elif kind == "refund":
            # Category of the original purchase, not an invented income source.
            categories[item.get("category") or "Uncategorized"] -= amount
    spent = totals["expense"] + totals["fee"] - totals["refund"]
    return {
        "month": month,
        "income": str(totals["income"]),
        "net_spending_after_refunds": str(spent),
        "loan_drawdowns_not_income": str(totals["loan_drawdown"]),
        "loan_repayments_not_expenses": str(totals["loan_repayment"]),
        "transfers_not_expenses": str(totals["transfer"] + totals["savings_transfer"]),
        "categories": {k: str(v) for k, v in sorted(categories.items())},
    }


def spending_changes(history: dict[str, dict[str, str]], current: dict[str, str], *, threshold: Decimal = Decimal("1.5")) -> list[dict]:
    """Descriptive anomaly candidates, never financial-accounting authority.

    Require three distinct months; compare current category net spend to median.
    Anomalies are not sent to the user or app automatically.
    """
    if len(history) < 3:
        return []
    changes = []
    categories = set().union(*(m.keys() for m in history.values()), current.keys())
    for category in sorted(categories):
        baseline = median([Decimal(history[m].get(category, "0")) for m in sorted(history)])
        observed = Decimal(current.get(category, "0"))
        if baseline > 0 and observed - baseline >= Decimal("100") and observed >= baseline * threshold:
            changes.append({"category": category, "historical_median": str(baseline), "observed": str(observed)})
    return changes


def goal_forecast(target: str, saved: str, monthly_contributions: list[str]) -> dict:
    """Simple *illustrative* savings estimate, not a guarantee or a money balance."""
    target_amount, saved_amount = _amount(target), Decimal(saved)
    if not saved_amount.is_finite() or saved_amount < 0 or saved_amount > target_amount:
        raise ValueError("saved amount is invalid")
    if not monthly_contributions:
        raise ValueError("at least one contribution is required")
    values = [Decimal(x) for x in monthly_contributions]
    if any(not v.is_finite() or v < 0 for v in values):
        raise ValueError("contributions must be nonnegative")
    outstanding = target_amount - saved_amount
    typical = Decimal(str(median(values)))
    months = 0 if outstanding == 0 else (int((outstanding/typical).to_integral_value(rounding="ROUND_CEILING")) if typical else None)
    return {
        "remaining": str(outstanding), "typical_monthly_contribution": str(typical),
        "estimated_months": months, "type": "illustrative_not_guaranteed",
    }


def demo() -> dict:
    """Read only repository-owned synthetic fixtures. No external I/O."""
    fixture = json.loads(FIXTURES.read_text(encoding="utf-8"))
    return {
        "message_experiment": evaluate(fixture["messages"]),
        "ledger_invariants": validate_ledger(fixture["ledger"]),
        "month_example": monthly_summary(fixture["ledger"], "2026-09"),
        "spending_signal": spending_changes(fixture["spending_history"], fixture["spending_current"]),
        "goal_example": goal_forecast("48000", "19000", ["7000", "7200", "7500"]),
        "production_connected": False,
    }


if __name__ == "__main__":
    print(json.dumps(demo(), indent=2, sort_keys=True))
