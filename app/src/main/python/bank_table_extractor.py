"""On-device, bounded table evidence for individually proven bank PDF layouts.

No network, file writes, OCR, logs or transaction/ledger mutations. Source
identifiers and financial cells are returned only to the local Kotlin caller.
"""
from __future__ import annotations

import base64
from decimal import Decimal, InvalidOperation
import io
import json
import re
import time

MAX_BYTES = 8 * 1024 * 1024
MAX_PAGES = 160
MAX_ROWS = 5000
MAX_WORDS = 32000
MAX_SECONDS = 120

PROFILES = {
    "KENYAN_BANK_EQUITY": {
        "issuer": "EQUITY",
        "currency": "KES",
        "headers": ("Date", "Value", "Particulars", "Money Out", "Money In", "Balance"),
        "date": re.compile(r"\d{2}-\d{2}-\d{4}\Z"),
        "signed_balance": re.compile(r"([\d,]+\.\d{2})\s+(Cr|Dr)\Z", re.I),
    }
}
MONEY = re.compile(r"(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d{1,2})?\Z")
TABLE_SETTINGS = {
    "vertical_strategy": "lines", "horizontal_strategy": "lines",
    "snap_tolerance": 3, "join_tolerance": 3, "intersection_tolerance": 3,
}


class Blocked(Exception):
    """The reason code must never contain a source value or identity."""


def _amount(text, *, optional=False):
    value = " ".join(text.split())
    if optional and value in ("", "-"):
        return 0
    if not MONEY.fullmatch(value):
        raise Blocked("invalid_money_cell")
    try:
        decimal = Decimal(value.replace(",", ""))
        minor = decimal * 100
        if minor != minor.to_integral_value() or not (0 <= minor < 2 ** 63):
            raise Blocked("invalid_money_precision")
        return int(minor)
    except (InvalidOperation, OverflowError):
        raise Blocked("invalid_money_precision") from None


def _balance(text, profile):
    match = profile["signed_balance"].fullmatch(" ".join(text.split()))
    if not match:
        raise Blocked("invalid_balance_cell")
    amount = _amount(match.group(1))
    return amount if match.group(2).lower() == "cr" else -amount


def _column(word, columns):
    owners = [i for i, (left, right) in enumerate(columns)
              if word["x0"] >= left - 0.6 and word["x1"] <= right + 0.6]
    return owners[0] if len(owners) == 1 else None


def _cells(words, columns):
    values = [[] for _ in columns]
    for word in words:
        index = _column(word, columns)
        if index is None:
            raise Blocked("unassigned_table_word")
        values[index].append(word)
    return [" ".join(w["text"].strip() for w in sorted(cell,
        key=lambda token: (token["top"], token["x0"]))) for cell in values]


def _label_y(words, label, after, required=True):
    first, second = label.split()
    found = []
    for word in words:
        if word["text"].upper() != first.upper() or word["top"] <= after:
            continue
        if any(abs(other["top"] - word["top"]) < 4 and
               0 <= other["x0"] - word["x1"] < 25 and
               other["text"].rstrip(":").upper() == second.upper() for other in words):
            found.append(word["top"])
    if len(found) > 1 or (required and not found):
        raise Blocked("missing_or_ambiguous_summary")
    return found[0] if found else None


def _issuer_account(words, profile):
    if not any(w["text"].upper() == profile["issuer"] and w["top"] < 300 for w in words):
        raise Blocked("unverified_issuer")
    if not any(w["text"].upper() == profile["currency"] and w["top"] < 320 for w in words):
        raise Blocked("unverified_currency")
    # A full account number must be printed on the same header line as ACCOUNT,
    # not an unrelated phone, reference, filename or bank title.
    candidates = set()
    for anchor in words:
        if anchor["text"].upper().strip(":") != "ACCOUNT" or not (90 < anchor["top"] < 320):
            continue
        for word in words:
            if (abs(word["top"] - anchor["top"]) < 4 and
                abs(word["x0"] - anchor["x1"]) < 360 and
                re.fullmatch(r"\d{8,20}", word["text"])):
                candidates.add(word["text"])
    if len(candidates) != 1:
        raise Blocked("unverified_source_account")
    return candidates.pop()


def _summary(words, columns, y, label, profile):
    nearby = [w for w in words if abs(w["top"] - y) < 4 and
              w["x0"] >= columns[0][0] - 1 and w["x1"] <= columns[-1][1] + 1]
    cells = _cells(nearby, columns)
    if label.upper() not in cells[0].upper():
        raise Blocked("misaligned_summary")
    return {"moneyOutMinor": _amount(cells[3]), "moneyInMinor": _amount(cells[4]),
            "balanceMinor": _balance(cells[5], profile)}


def _read_page(page, page_number, profile, deadline):
    if time.monotonic() > deadline:
        raise Blocked("review_timeout")
    words = page.extract_words(x_tolerance=2, y_tolerance=3)
    if not words or len(words) > MAX_WORDS:
        raise Blocked("image_or_oversize_page")
    source_id = _issuer_account(words, profile)
    tables = page.find_tables(TABLE_SETTINGS)
    if len(tables) != 1 or len(tables[0].rows) < 2:
        raise Blocked("unverified_physical_table")
    header_cells = tables[0].rows[0].cells
    if len(header_cells) != 6 or any(cell is None for cell in header_cells):
        raise Blocked("unverified_columns")
    columns = [(cell[0], cell[2]) for cell in header_cells]
    if any(a >= b for a, b in columns) or any(columns[i][1] > columns[i + 1][0] + 1
                                               for i in range(5)):
        raise Blocked("ambiguous_columns")
    printed_headers = tables[0].extract()[0]
    if len(printed_headers) != 6 or any(
        " ".join((printed_headers[i] or "").split()).lower() != name.lower()
        for i, name in enumerate(profile["headers"]) if i > 0):
        raise Blocked("unverified_header")
    # Some issuer PDFs layer the Date heading on an overlapping text object.
    # The first page must carry the intact header; transaction rows must have
    # unambiguous date anchors in the first physical column.
    exact_date_heading = " ".join((printed_headers[0] or "").split()).lower() == "date"
    header_bottom = header_cells[0][3]
    dated = sorted([w for w in words if w["top"] > header_bottom + 3 and
        w["top"] < tables[0].bbox[3] and _column(w, columns) == 0 and
        profile["date"].fullmatch(w["text"])], key=lambda w: w["top"])
    if len(dated) > MAX_ROWS or any(b["top"] - a["top"] < 4 for a, b in zip(dated, dated[1:])):
        raise Blocked("ambiguous_date_anchors")
    if not dated:
        if not exact_date_heading:
            raise Blocked("unverified_cover_table")
        # A cover table is permitted only if its body contains no financial
        # amounts, balance suffixes or date anchors; it is not a hidden leg.
        body = [w for w in words if header_bottom + 3 < w["top"] < tables[0].bbox[3]
                and columns[0][0] <= w["x0"] <= columns[-1][1]]
        if any(MONEY.fullmatch(w["text"]) or w["text"].lower() in ("cr", "dr")
               for w in body):
            raise Blocked("unaccounted_cover_amount")
        return source_id, None
    page_y = _label_y(words, "Page Total", dated[-1]["top"], required=False)
    grand_y = _label_y(words, "Grand Total", dated[-1]["top"], required=False)
    if page_y is None and grand_y is None:
        raise Blocked("missing_or_ambiguous_summary")
    if page_y is not None and grand_y is not None and page_y >= grand_y:
        raise Blocked("invalid_source_regions")
    summary_positions = [y for y in (page_y, grand_y) if y is not None]
    first_footer = min(summary_positions)
    uncleared_y = _label_y(words, "Uncleared Cheques", max(summary_positions))
    if not (header_bottom < dated[0]["top"] <= dated[-1]["top"] < first_footer < uncleared_y):
        raise Blocked("invalid_source_regions")
    body = [w for w in words if header_bottom + 3 < w["top"] < first_footer - 4 and
            columns[0][0] - 1 <= w["x0"] < columns[-1][1] + 1]
    # This issuer prints one free-text notice immediately before Page Total,
    # spanning several columns. Classify it explicitly as nonfinancial source
    # evidence; never silently drop an unknown word or a numeric fragment.
    unowned = [w for w in body if _column(w, columns) is None]
    if unowned:
        note_y = unowned[0]["top"]
        notice = [w for w in body if abs(w["top"] - note_y) < 3]
        if (any(abs(w["top"] - note_y) >= 3 for w in unowned) or
            not (dated[-1]["top"] + 6 < note_y < first_footer - 5) or
            min(w["x0"] for w in notice) >= columns[0][1] or
            max(w["x1"] for w in notice) <= columns[2][1] or
            not (20 <= sum(len(w["text"]) for w in notice) <= 400) or
            any(char.isdigit() for w in notice for char in w["text"])):
            raise Blocked("unassigned_table_word")
        body = [w for w in body if w not in notice]
    earlier = [w for w in body if w["top"] < dated[0]["top"] - 3]
    previous_cells = _cells(earlier, columns)
    if any(previous_cells[i].strip() for i in range(5)):
        raise Blocked("unknown_opening_adjustment")
    opening = _balance(previous_cells[5], profile)
    source_rows = []
    previous_balance = opening
    for i, date_anchor in enumerate(dated):
        start = date_anchor["top"] - 3
        end = dated[i + 1]["top"] - 3 if i + 1 < len(dated) else first_footer - 4
        row = _cells([w for w in body if start <= w["top"] < end], columns)
        if row[0] != date_anchor["text"] or row[1].strip():
            raise Blocked("unsupported_value_or_date_column")
        if not row[2].strip() or len(row[2]) > 200:
            raise Blocked("unsupported_description")
        outgoing = _amount(row[3], optional=True)
        incoming = _amount(row[4], optional=True)
        if (outgoing == 0) == (incoming == 0):
            raise Blocked("ambiguous_cash_leg")
        balance = _balance(row[5], profile)
        if previous_balance + incoming - outgoing != balance:
            raise Blocked("balance_mismatch")
        previous_balance = balance
        source_rows.append(row)
    page_total = _summary(words, columns, page_y, "Page Total", profile) if page_y is not None else None
    grand_total = _summary(words, columns, grand_y, "Grand Total", profile) if grand_y is not None else None
    # Nonzero uncleared cheques are not posted cash legs and need a separate policy.
    uncleared_values = [w["text"] for w in words if abs(w["top"] - uncleared_y) < 4 and
                        columns[3][0] <= w["x0"] < columns[-1][1]]
    if len(uncleared_values) != 1 or _amount(uncleared_values[0]) != 0:
        raise Blocked("unsupported_uncleared_cheques")
    income = sum(_amount(row[4], optional=True) for row in source_rows)
    outgoing = sum(_amount(row[3], optional=True) for row in source_rows)
    if any(total["moneyInMinor"] != income or total["moneyOutMinor"] != outgoing or
           total["balanceMinor"] != previous_balance for total in (page_total, grand_total) if total is not None):
        raise Blocked("printed_totals_mismatch")
    return source_id, {
        "pageNumber": page_number, "headers": profile["headers"],
        "rows": source_rows, "openingBalanceMinor": opening,
        "pageTotal": {"moneyInMinor": income, "moneyOutMinor": outgoing} if page_total is not None else None,
        "grandTotal": {"moneyInMinor": income, "moneyOutMinor": outgoing} if grand_total is not None else None,
    }


def inspect_pdf(base64_data: str, password: str, provider_key: str) -> str:
    """JSON boundary: no partial source may ever be marked ready for review."""
    profile = PROFILES.get(provider_key)
    if profile is None:
        return json.dumps({"status": "blocked", "reason": "unsupported_provider"})
    stage = "pdf_input_failed"
    try:
        if len(base64_data) > 12 * 1024 * 1024 or len(password) > 256:
            raise Blocked("oversize_or_invalid_input")
        raw = base64.b64decode(base64_data, validate=True)
        if len(raw) > MAX_BYTES or not raw.startswith(b"%PDF-"):
            raise Blocked("invalid_pdf")
        stage = "pdf_engine_unavailable"
        import pdfplumber  # bundled by Chaquopy; import only after bounded input.
        deadline = time.monotonic() + MAX_SECONDS
        stage = "pdf_open_failed"
        with pdfplumber.open(io.BytesIO(raw), password=password) as pdf:
            if len(pdf.pages) not in range(1, MAX_PAGES + 1):
                raise Blocked("unsupported_pages")
            identity = None
            tables = []
            for index, page in enumerate(pdf.pages, 1):
                stage = "pdf_page_extraction_failed"
                try:
                    source_id, result = _read_page(page, index, profile, deadline)
                except Blocked as original:
                    if str(original) != "unverified_issuer" or provider_key != "KENYAN_BANK_EQUITY":
                        raise
                    from equity_legacy_extractor import LegacyBlocked, inspect_legacy
                    try:
                        return json.dumps(inspect_legacy(pdf, profile, deadline))
                    except LegacyBlocked as legacy:
                        raise Blocked(str(legacy)) from None
                if identity is not None and identity != source_id:
                    raise Blocked("account_changes_between_pages")
                identity = source_id
                if result is not None:
                    tables.append(result)
            if len(tables) != 1 or sum(len(t["rows"]) for t in tables) not in range(1, MAX_ROWS + 1):
                raise Blocked("unverified_statement_coverage")
            source = tables[0]
            return json.dumps({"status": "ready_for_bank_review", "document": {
                "providerKey": provider_key, "accountIdentifier": identity,
                "currency": profile["currency"], "format": "PDF", "pages": len(pdf.pages),
                "openingBalanceMinor": source["openingBalanceMinor"],
                "printedPageTotals": ({str(source["pageNumber"]): source["pageTotal"]}
                                      if source["pageTotal"] is not None else {}),
                "printedGrandTotal": source["grandTotal"],
                "allSourceRegionsAccountedFor": True,
            }, "tables": [{"pageNumber": source["pageNumber"],
                           "headers": source["headers"], "rows": source["rows"]}]})
    except Blocked as problem:
        return json.dumps({"status": "blocked", "reason": str(problem)})
    except Exception as problem:
        # Only stable stage/class codes may cross the boundary. Exceptions can
        # include private source tokens, document identifiers, or passwords.
        from pdf_opening_error import requires_password
        reason = "pdf_password_rejected" if requires_password(problem) else stage
        return json.dumps({"status": "blocked", "reason": reason})
