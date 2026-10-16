"""Exact, bounded financial ledger proof from yesterday's offline M-Pesa audits.

Rows are in source order (newest first). Never remove or mutate a cash leg.
A narrowly identified zero-net Fuliza pair is checked separately only when
BOTH original neighboring timestamp boundaries are disconnected and the wallet
timeline bypasses the pair exactly. That balance-scope model is provisional.
"""
from decimal import Decimal
from collections import defaultdict
import re

ZERO_EVENT_LABELS = {
    "m-shwari loan request",
    "m-shwari lock withdraw to savings",
    "m-shwari lock activate and save from savings",
}


def _label(value):
    return " ".join(value.lower().split())


def _is_fuliza_pair(rows):
    if len(rows) != 2 or len({r["receipt"] for r in rows}) != 1:
        return False
    if len({r["when"] for r in rows}) != 1:
        return False
    incoming = [r for r in rows if r["delta"] > 0]
    outgoing = [r for r in rows if r["delta"] < 0]
    if len(incoming) != 1 or len(outgoing) != 1:
        return False
    credit, debit = incoming[0], outgoing[0]
    return (credit["delta"] + debit["delta"] == 0
            and credit["balance"] == credit["delta"]
            and debit["balance"] == 0
            and "overdraft of credit party" in _label(credit["description"])
            and "fuliza" in _label(debit["description"])
            and ("paybill" in _label(debit["description"])
                 or "pay bill" in _label(debit["description"])))


def _orders(block):
    if len(block) > 9:
        raise ValueError("timestamp_block_limit")
    result = []
    used = [False] * len(block)

    def visit(depth, newest, previous):
        if depth == len(block):
            result.append((newest, previous["balance"] - previous["delta"]))
            if len(result) >= 2048:
                raise ValueError("timestamp_candidate_limit")
            return
        for index, row in enumerate(block):
            if used[index]:
                continue
            if previous is not None and previous["balance"] - row["balance"] != previous["delta"]:
                continue
            used[index] = True
            visit(depth + 1, row["balance"] if not depth else newest, row)
            used[index] = False

    visit(0, None, None)
    if not result:
        raise ValueError("timestamp_internal_balance")
    return result


def _linked(newer, older):
    return any(a[1] == b[0] for a in newer for b in older)


def _unique_path(options):
    if not options:
        raise ValueError("missing_financial_movements")
    ways = [[0] * len(candidates) for candidates in options]
    ways[-1] = [1] * len(options[-1])
    for i in range(len(options) - 2, -1, -1):
        for j, newer in enumerate(options[i]):
            ways[i][j] = min(2, sum(
                ways[i + 1][k] for k, older in enumerate(options[i + 1])
                if newer[1] == older[0]))
    if sum(ways[0]) != 1:
        raise ValueError("wallet_chain_ambiguous_or_disconnected")
    index = ways[0].index(1)
    chosen = [options[0][index]]
    for i in range(1, len(options)):
        matching = [j for j, candidate in enumerate(options[i])
                    if ways[i][j] and chosen[-1][1] == candidate[0]]
        if len(matching) != 1:
            raise ValueError("wallet_chain_nonunique")
        chosen.append(options[i][matching[0]])
    return chosen


def reconcile(rows):
    if not rows or len(rows) > 5000:
        raise ValueError("financial_row_limit")
    if any(a["when"] < b["when"] for a, b in zip(rows, rows[1:])):
        raise ValueError("source_timestamp_order")
    infos = [r for r in rows if r["delta"] == 0]
    if any(_label(r["description"]) not in ZERO_EVENT_LABELS for r in infos):
        raise ValueError("unknown_zero_informational_event")

    # Never use receipt as a financial-row deduplication key.
    groups = []
    observed = set()
    at = 0
    while at < len(rows):
        end = at + 1
        while end < len(rows) and rows[end]["receipt"] == rows[at]["receipt"]:
            end += 1
        group = rows[at:end]
        if group[0]["receipt"] in observed or len(group) > 12 or any(
                r["when"] != group[0]["when"] for r in group):
            raise ValueError("noncontiguous_or_invalid_receipt")
        observed.add(group[0]["receipt"])
        groups.append(group)
        at = end

    units = []
    by_receipt = {}
    shared = 0
    for group in groups:
        money = [r for r in group if r["delta"] != 0]
        by_receipt[group[0]["receipt"]] = money
        if not money:
            continue
        if len(money) > 1 and len({r["balance"] for r in money}) == 1:
            first = dict(money[0])
            first["delta"] = sum((r["delta"] for r in money), Decimal(0))
            units.append(first)
            shared += 1
        else:
            units.extend(money)

    blocks = []
    at = 0
    while at < len(units):
        end = at + 1
        while end < len(units) and units[end]["when"] == units[at]["when"]:
            end += 1
        blocks.append(units[at:end])
        at = end
    if not blocks:
        raise ValueError("no_cash_rows")

    options = [_orders(block) for block in blocks]
    exceptions = []
    for i in range(1, len(blocks) - 1):
        block = blocks[i]
        if len(block) != 2 or block[0]["receipt"] != block[1]["receipt"]:
            continue
        group = by_receipt[block[0]["receipt"]]
        if (_is_fuliza_pair(group) and not _linked(options[i-1], options[i])
                and not _linked(options[i], options[i+1])
                and _linked(options[i-1], options[i+1])):
            exceptions.append(i)
    if any(b-a <= 1 for a,b in zip(exceptions, exceptions[1:])):
        raise ValueError("overlapping_fuliza_exceptions")

    removed = set(exceptions)
    active = [block for i, block in enumerate(blocks) if i not in removed]
    choices = [opt for i, opt in enumerate(options) if i not in removed]
    path = _unique_path(choices)
    opening = path[-1][1]
    closing = path[0][0]
    wallet_net = sum((r["delta"] for r in rows), Decimal(0))
    if min(opening, closing) < 0 or closing - opening != wallet_net:
        raise ValueError("wallet_endpoint_net_mismatch")
    for info in infos:
        same = next((i for i,b in enumerate(active) if b[0]["when"] == info["when"]), -1)
        if same >= 0:
            compatible = (info["balance"] == path[same][1]
                          or any(unit["balance"] == info["balance"] for unit in active[same]))
        else:
            between = next((i for i in range(len(active)-1)
                            if active[i][0]["when"] > info["when"] > active[i+1][0]["when"]), -1)
            compatible = between >= 0 and info["balance"] == path[between][1]
        if not compatible:
            raise ValueError("informational_balance_mismatch")

    return {
        "source_financial_rows": len(rows) - len(infos),
        "source_informational_rows": len(infos),
        "balance_checking_units": len(units),
        "shared_ending_balance_receipts": shared,
        "fuliza_separately_checked": len(exceptions),
        "timestamp_blocks_original": len(blocks),
        "wallet_chain_unique_provisionally": True,
        "derived_opening_minor": int(opening * 100),
        "derived_closing_minor": int(closing * 100),
        "source_opening_closing_independently_verified": False,
        "external_receipt_count_independently_verified": False,
        "fuliza_balance_scope_independently_confirmed": False,
    }
