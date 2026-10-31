"""Narrow, no-network reader for the historical graphical-header Equity PDF.

The extractor establishes row coverage, a printed grand Total and adjacent
balance consistency. It does NOT prove the absent opening/page totals or the
issuer logo's authenticity. Callers must label this lower assurance accordingly.
"""
from __future__ import annotations

from datetime import datetime
from decimal import Decimal
import re
import time

DATE = re.compile(r"\d{2}/\d{2}/\d{4}\Z")
MONEY = re.compile(r"(?:\d{1,3}(?:,\d{3})+|\d+)\.\d{2}\Z")
FULL_ACCOUNT = re.compile(r"\d{8,20}\Z")
MAX_ROWS = 5000


class LegacyBlocked(Exception):
    """Only static reason codes, never source data."""


def _unlayer(value):
    if len(value) % 2 == 0 and value[::2] == value[1::2]:
        return value[::2]
    return value


def _label(value):
    return re.sub(r"[^a-z]", "", _unlayer(value).lower())


def _money(value):
    value = _unlayer(value)
    if not MONEY.fullmatch(value):
        raise LegacyBlocked("legacy_money_ambiguous")
    amount = int(Decimal(value.replace(",", "")) * 100)
    if amount < 0 or amount >= 2 ** 63:
        raise LegacyBlocked("legacy_money_out_of_range")
    return amount


def _headings(words, page_number):
    top = 180 if page_number == 1 else 7
    checks = (
        (37, "transaction"), (246, "payment"), (324, "value"),
        (393, "credit"), (465, "debit"), (537, "balance"),
    )
    for x, expected in checks:
        if not any(abs(w["top"] - top) < 5 and abs(w["x0"] - x) < 6
                   and _label(w["text"]) == expected for w in words):
            raise LegacyBlocked("legacy_columns_unverified")
    return top


def _identity(first_page, words):
    if not any(30 <= image["x0"] <= 70 and 20 <= image["top"] <= 50
               and 25 <= image["width"] <= 120 for image in first_page.images):
        raise LegacyBlocked("legacy_graphical_header_missing")
    line = [w for w in words if w["top"] < 34]
    has_account = any(_label(w["text"]) == "account" and 375 < w["x0"] < 405 for w in line)
    has_number = any(_label(w["text"]) == "number" and 405 < w["x0"] < 445 for w in line)
    ids = {w["text"] for w in line if w["x0"] >= 440 and FULL_ACCOUNT.fullmatch(w["text"])}
    if not has_account or not has_number or len(ids) != 1:
        raise LegacyBlocked("legacy_account_unverified")
    if not any(w["text"] == "KES" and w["top"] < 60 and w["x0"] >= 440 for w in words):
        raise LegacyBlocked("legacy_currency_unverified")
    return ids.pop()


def _cash_cells(words, anchor):
    cash = [w for w in words if abs(w["top"] - anchor["top"]) < 4
            and MONEY.fullmatch(w["text"])]
    credit = [w for w in cash if 385 < w["x0"] < 465]
    debit = [w for w in cash if 465 <= w["x0"] < 530]
    balance = [w for w in cash if w["x0"] >= 530]
    if len(balance) != 1 or len(credit) + len(debit) != 1:
        raise LegacyBlocked("legacy_cash_leg_ambiguous")
    signed = _money(credit[0]["text"]) if credit else -_money(debit[0]["text"])
    if signed == 0:
        raise LegacyBlocked("legacy_zero_cash_leg")
    return signed, _money(balance[0]["text"])


def _rows_on_page(number, page_count, words, heading):
    anchors = sorted((w for w in words if 315 <= w["x0"] < 358 and
                      w["top"] > heading + 12 and w["top"] < 765 and
                      DATE.fullmatch(w["text"])), key=lambda w: w["top"])
    if not anchors:
        raise LegacyBlocked("legacy_page_has_no_transactions")
    if any(b["top"] - a["top"] < 24 for a, b in zip(anchors, anchors[1:])):
        raise LegacyBlocked("legacy_duplicate_date_anchor")
    # A money cell without a dated transaction must never disappear silently.
    # The only permitted exception is the separately checked final Total row.
    total_y = [w["top"] for w in words if number == page_count and w["x0"] < 80
               and w["top"] > anchors[-1]["top"] + 20 and _label(w["text"]) == "total"]
    cash_words = (w for w in words if 385 < w["x0"] < 590
                  and heading + 12 < w["top"] < 760
                  and MONEY.fullmatch(_unlayer(w["text"])))
    for cash in cash_words:
        if any(abs(cash["top"] - anchor["top"]) < 4 for anchor in anchors):
            continue
        if len(total_y) == 1 and abs(cash["top"] - total_y[0]) < 4:
            continue
        raise LegacyBlocked("legacy_unaccounted_cash_cell")
    table = []
    entries = []
    for i, anchor in enumerate(anchors):
        top = anchor["top"]
        begin = top - 20
        end = min(top + 32, anchors[i + 1]["top"] - 20) if i + 1 < len(anchors) else top + 32
        details = [w for w in words if begin <= w["top"] < end and 35 <= w["x0"] < 240]
        description = " ".join(w["text"] for w in sorted(details, key=lambda w: (w["top"], w["x0"])))
        references = [w["text"] for w in words if abs(w["top"] - top) < 4
                      and 242 < w["x0"] < 315]
        if not description or len(description) > 200 or len(references) != 1:
            raise LegacyBlocked("legacy_description_or_reference_ambiguous")
        try:
            date = datetime.strptime(anchor["text"], "%d/%m/%Y")
        except ValueError:
            raise LegacyBlocked("legacy_date_invalid") from None
        signed, balance = _cash_cells(words, anchor)
        incoming = f"{signed // 100}.{signed % 100:02d}" if signed > 0 else ""
        outgoing = f"{(-signed) // 100}.{(-signed) % 100:02d}" if signed < 0 else ""
        table.append([description, references[0], anchor["text"], incoming,
                      outgoing, f"{balance // 100}.{balance % 100:02d}"])
        entries.append((date, signed, balance))
    if len(table) > MAX_ROWS:
        raise LegacyBlocked("legacy_row_limit")
    return table, entries


def _printed_grand_total(words, last_balance, incoming, outgoing, last_anchor_top):
    label = [w for w in words if w["x0"] < 80 and w["top"] > last_anchor_top + 20
             and w["top"] < 760 and _label(w["text"]) == "total"]
    if len(label) != 1:
        raise LegacyBlocked("legacy_grand_total_missing")
    y = label[0]["top"]
    cells = [w for w in words if abs(w["top"] - y) < 4]
    def one(left, right):
        matches = [w for w in cells if left <= w["x0"] < right and
                   MONEY.fullmatch(_unlayer(w["text"]))]
        if len(matches) != 1:
            raise LegacyBlocked("legacy_grand_total_ambiguous")
        return _money(matches[0]["text"])
    if (one(393, 465) != incoming or one(465, 535) != outgoing or
            one(535, 590) != last_balance):
        raise LegacyBlocked("legacy_grand_total_mismatch")


def inspect_legacy(pdf, profile, deadline):
    """Return the bank review object or raise a stable, privacy-safe reason."""
    if profile["issuer"] != "EQUITY" or not (2 <= len(pdf.pages) <= 160):
        raise LegacyBlocked("legacy_layout_unrecognized")
    account = None
    pages = []
    all_entries = []
    all_words = []
    for number, page in enumerate(pdf.pages, 1):
        if time.monotonic() > deadline:
            raise LegacyBlocked("review_timeout")
        if not (590 <= page.width <= 600 and 835 <= page.height <= 850):
            raise LegacyBlocked("legacy_page_geometry_unknown")
        words = page.extract_words(x_tolerance=2, y_tolerance=3)
        if not words or len(words) > 32000:
            raise LegacyBlocked("legacy_text_layer_missing")
        if number == 1:
            account = _identity(page, words)
        heading = _headings(words, number)
        rows, entries = _rows_on_page(number, len(pdf.pages), words, heading)
        pages.append({"pageNumber": number,
                      "headers": ["Transaction Details", "Payment reference",
                                  "Date", "Credit", "Debit", "Balance"],
                      "rows": rows})
        all_entries.extend(entries)
        all_words.append(words)
    if not (2 <= len(all_entries) <= MAX_ROWS):
        raise LegacyBlocked("legacy_transaction_coverage_incomplete")
    if any(a[0] > b[0] for a, b in zip(all_entries, all_entries[1:])):
        raise LegacyBlocked("legacy_date_order_invalid")
    if any(a[2] + b[1] != b[2] for a, b in zip(all_entries, all_entries[1:])):
        raise LegacyBlocked("legacy_balance_chain_mismatch")
    incoming = sum(e[1] for e in all_entries if e[1] > 0)
    outgoing = -sum(e[1] for e in all_entries if e[1] < 0)
    if incoming >= 2 ** 63 or outgoing >= 2 ** 63:
        raise LegacyBlocked("legacy_total_out_of_range")
    last_words = all_words[-1]
    last_anchors = [w["top"] for w in last_words if 315 <= w["x0"] < 358 and
                    DATE.fullmatch(w["text"]) and w["top"] > 19]
    _printed_grand_total(last_words, all_entries[-1][2], incoming, outgoing,
                         max(last_anchors))
    return {"status": "ready_for_bank_review", "document": {
        "providerKey": "KENYAN_BANK_EQUITY", "accountIdentifier": account,
        "currency": "KES", "format": "PDF", "pages": len(pdf.pages),
        "assurance": "grand_total_and_chain_only",
        "openingBalanceMinor": None, "printedPageTotals": {},
        "printedGrandTotal": {"moneyInMinor": incoming, "moneyOutMinor": outgoing},
        "allSourceRegionsAccountedFor": True,
    }, "tables": pages}
