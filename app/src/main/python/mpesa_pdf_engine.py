"""Offline, read-only M-Pesa physical-table importer for Android via Chaquopy.

Ported from the verified Windows pdfplumber workflow: physical line grids,
seven source columns, center-owned per-cell characters, the official summary,
and exact wallet-balance checks. No OCR, cloud, file writes or ledger updates.
Only verified source rows are returned; financial ledger import is forbidden.
"""
from __future__ import annotations
import base64
from bisect import bisect_right
from collections import Counter
from datetime import datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
import io
import json
import re

from mpesa_wallet_reconcile import reconcile

MAX_PAGES = 160
MAX_BYTES = 8 * 1024 * 1024
MAX_ROWS = 5000
TABLE_SETTINGS = {
    "vertical_strategy": "lines", "horizontal_strategy": "lines",
    "snap_tolerance": 3, "join_tolerance": 3, "intersection_tolerance": 3
}
HEADERS = ("receiptno", "completiontime", "details", "transactionstatus",
           "paidin", "withdrawn", "balance")
MONEY = re.compile(r"-?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?\Z|-?\d+(?:\.\d{1,2})?\Z")
RECEIPT = re.compile(r"[A-Za-z0-9]{10}\Z")
DATETIMES = ("%Y-%m-%d %H:%M:%S", "%d/%m/%Y %H:%M:%S",
             "%d-%m-%Y %H:%M:%S", "%d/%m/%y %H:%M:%S")
EAT = timezone(timedelta(hours=3))


def _compact(s):
    return "".join(str(s).split())


def _norm(s):
    return re.sub(r"[^a-z0-9]", "", str(s).lower())


def _money(value):
    text = str(value or "").strip().replace("\u2212", "-")
    if text in ("", "-", "\u2014"):
        return None
    if not MONEY.fullmatch(text):
        raise ValueError("invalid_source_money")
    out = Decimal(text.replace(",", ""))
    if not out.is_finite() or out.as_tuple().exponent < -2:
        raise ValueError("invalid_source_money_precision")
    return out


def _minor(value):
    return int(value * 100)


def _cell(page, rect):
    if rect is None or len(rect) != 4:
        raise ValueError("missing_table_cell")
    text = page.crop(tuple(rect), strict=False).extract_text(
        x_tolerance=1.5, y_tolerance=3)
    return (text or "").strip()


def _date(value):
    text = " ".join(value.split())
    for pattern in DATETIMES:
        try:
            return datetime.strptime(text, pattern)
        except ValueError:
            pass
    raise ValueError("unsupported_source_datetime")


def _validate_source_chars(page, values, rectangles):
    """Each nonblank source glyph belongs to its exact 7-column physical cell."""
    if not rectangles:
        raise ValueError("empty_table_body")
    xs = [rectangles[0][0][0]] + [rect[2] for rect in rectangles[0]]
    ys = [rectangles[0][0][1]] + [row[0][3] for row in rectangles]
    if len(xs) != 8 or len(ys) != len(rectangles)+1:
        raise ValueError("invalid_physical_grid")
    if any(a >= b for a,b in zip(xs,xs[1:])) or any(
            a >= b for a,b in zip(ys,ys[1:])):
        raise ValueError("nonmonotonic_grid")
    slots = [[[] for _ in range(7)] for _ in rectangles]
    count = 0
    for char in page.chars:
        glyph = char.get("text", "")
        if not glyph.strip():
            continue
        center_x = (char["x0"] + char["x1"]) / 2
        center_y = (char["top"] + char["bottom"]) / 2
        if not (xs[0] <= center_x < xs[-1] and ys[0] <= center_y < ys[-1]):
            continue
        row = bisect_right(ys, center_y) - 1
        col = bisect_right(xs, center_x) - 1
        if not (0 <= row < len(rectangles) and 0 <= col < 7):
            raise ValueError("unowned_source_character")
        if (char["x0"] < xs[col] or char["x1"] > xs[col+1]
                or char["top"] < ys[row] or char["bottom"] > ys[row+1]):
            raise ValueError("crossing_physical_cell")
        slots[row][col].append(glyph)
        count += len(_compact(glyph))
    for index, cells in enumerate(values):
        for col, cell in enumerate(cells):
            if _compact(cell) != _compact("".join(slots[index][col])):
                raise ValueError("cell_character_inventory_mismatch")
    return count


def _physical_rows(page):
    """Require exactly one physically evidenced detailed table per page."""
    matches = []
    for table in page.find_tables(table_settings=TABLE_SETTINGS):
        if len(table.columns) != 7:
            continue
        raw = [list(row.cells) for row in table.rows]
        if any(len(row) != 7 or any(c is None for c in row) for row in raw):
            continue
        header_rows = []
        for i, row in enumerate(raw):
            names = tuple(_norm(_cell(page, rect)) for rect in row)
            if names == HEADERS:
                header_rows.append(i)
        if len(header_rows) != 1:
            continue
        start = header_rows[0] + 1
        rectangles = raw[start:]
        if not rectangles:
            continue
        values = [[_cell(page, rect) for rect in row] for row in rectangles]
        if any(not row[0].strip() or not RECEIPT.fullmatch(
                _compact(row[0])) for row in values):
            continue
        matches.append((values, rectangles))
    if len(matches) != 1:
        raise ValueError("physical_seven_column_table_not_unique")
    values, rectangles = matches[0]
    return values, _validate_source_chars(page, values, rectangles)


def _summary(page):
    """Independently read Safaricom's physical three-column summary table."""
    matches = []
    for table in page.find_tables(table_settings=TABLE_SETTINGS):
        if len(table.columns) != 3:
            continue
        raw = [list(row.cells) for row in table.rows]
        if not raw or any(len(row) != 3 or any(c is None for c in row) for row in raw):
            continue
        values = [[_cell(page, cell) for cell in row] for row in raw]
        headers = [i for i, row in enumerate(values) if
                   tuple(_norm(c) for c in row) == ("transactiontype", "paidin", "paidout")]
        if len(headers) != 1:
            continue
        categories, total = [], []
        for line in values[headers[0]+1:]:
            label = _norm(line[0])
            if not label:
                continue
            try:
                paid, withdrawn = _money(line[1]), _money(line[2])
            except ValueError:
                break
            if paid is None or withdrawn is None or paid < 0 or withdrawn < 0:
                break
            if label == "total":
                total.append((paid, withdrawn))
            else:
                categories.append((label, paid, withdrawn))
        if len(total) == 1 and len(categories) >= 3:
            matches.append((categories, total[0]))
    if len(matches) != 1:
        raise ValueError("official_statement_summary_not_unique")
    return matches[0]


def extract_bytes(raw: bytes, password: str | None = None):
    import pdfplumber

    if not isinstance(raw, bytes) or not 0 < len(raw) <= MAX_BYTES or not raw.startswith(b"%PDF-"):
        raise ValueError("invalid_or_oversize_pdf")
    parsed = []
    source_chars = 0
    with pdfplumber.open(io.BytesIO(raw), password=password or "") as document:
        pages = len(document.pages)
        if pages < 1 or pages > MAX_PAGES:
            raise ValueError("unsupported_document_page_count")
        categories, official = _summary(document.pages[0])
        for page_no, page in enumerate(document.pages, 1):
            grid, chars = _physical_rows(page)
            source_chars += chars
            for row_num, cells in enumerate(grid, 1):
                if len(parsed) >= MAX_ROWS:
                    raise ValueError("source_row_limit")
                reference = _compact(cells[0]).upper()
                when = _date(cells[1])
                detail = " ".join(cells[2].split())
                status = _norm(cells[3])
                incoming, outgoing = _money(cells[4]), _money(cells[5])
                balance = _money(cells[6])
                if (not RECEIPT.fullmatch(reference) or status != "completed" or
                        not 0 < len(detail) <= 200 or balance is None or balance < 0):
                    raise ValueError("invalid_source_row_semantics")
                if incoming is not None and incoming > 0 and outgoing is None:
                    delta = incoming
                elif outgoing is not None and outgoing < 0 and incoming is None:
                    delta = outgoing
                elif (incoming == 0 and outgoing is None) or (
                        outgoing == 0 and incoming is None):
                    delta = Decimal(0)
                else:
                    raise ValueError("ambiguous_cash_leg")
                parsed.append({
                    "receipt": reference, "when": when, "description": detail,
                    "delta": delta, "balance": balance,
                    "source_cells": list(cells),
                    "page": page_no, "row": row_num
                })
    if not parsed:
        raise ValueError("no_source_rows")
    paid = sum((r["delta"] for r in parsed if r["delta"] > 0), Decimal(0))
    withdrawn = sum((-r["delta"] for r in parsed if r["delta"] < 0), Decimal(0))
    if (paid, withdrawn) != official:
        raise ValueError("detailed_totals_mismatch_official_summary")
    financial = reconcile(parsed)

    lines = [{
        "page": r["page"], "row": r["row"], "reference": r["receipt"],
        "occurredAt": int(r["when"].replace(tzinfo=EAT).timestamp() * 1000),
        "description": r["description"],
        "paidInMinor": _minor(r["delta"]) if r["delta"] > 0 else 0,
        "withdrawnMinor": -_minor(r["delta"]) if r["delta"] < 0 else 0,
        "balanceMinor": _minor(r["balance"]),
        "sourceCells": r["source_cells"]
    } for r in parsed]
    category_in = sum((r[1] for r in categories), Decimal(0))
    category_out = sum((r[2] for r in categories), Decimal(0))
    return {
        "status": "ready_for_separate_activity_review",
        "engine": "pdfplumber-0.11.9",
        "report": {
            "pages": pages, "sourceRows": len(parsed), "financialRows": financial["source_financial_rows"],
            "informationalRows": financial["source_informational_rows"],
            "sourceCharacters": source_chars,
            "paidInMinor": _minor(paid), "paidOutMinor": _minor(withdrawn),
            "summaryDetailedTotalsMatch": True,
            "categoryPaidInDiscrepancyMinor": _minor(official[0] - category_in),
            "categoryPaidOutDiscrepancyMinor": _minor(official[1] - category_out),
            **financial,
            "verified_for_sms_ledger_import": False,
            "original_pdf_modified": False,
        },
        "lines": lines
    }


def inspect_pdf(base64_pdf: str, password: str = "") -> str:
    """Android entrypoint. Fail closed with non-sensitive error categories."""
    try:
        blob = base64.b64decode(base64_pdf, validate=True)
        return json.dumps(extract_bytes(blob, password), separators=(",", ":"))
    except Exception as exc:
        # No customer data, source text, file name or password crosses on error.
        allowed = {
            "physical_seven_column_table_not_unique", "official_statement_summary_not_unique",
            "detailed_totals_mismatch_official_summary", "cell_character_inventory_mismatch",
            "source_timestamp_order", "unknown_zero_informational_event",
            "wallet_chain_ambiguous_or_disconnected", "invalid_or_oversize_pdf",
            "source_row_limit", "ambiguous_cash_leg", "invalid_source_row_semantics",
            "informational_balance_mismatch", "crossing_physical_cell",
        }
        category = str(exc) if isinstance(exc, ValueError) and str(exc) in allowed else "unsupported_or_unverified_pdf"
        return json.dumps({
            "status": "blocked", "reason": category,
            "verified_for_sms_ledger_import": False
        }, separators=(",", ":"))
