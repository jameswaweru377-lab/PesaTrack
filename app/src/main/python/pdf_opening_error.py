"""Recognize a PDF opening-password failure without inspecting private error strings."""


def requires_password(problem: BaseException) -> bool:
    # pdfplumber wraps PDFMiner's typed error in PdfminerException.__context__.
    # Do not inspect str(problem): it can include private document information.
    seen = set()
    pending = [problem]
    while pending:
        current = pending.pop()
        if current is None or id(current) in seen:
            continue
        seen.add(id(current))
        if type(current).__name__ == "PDFPasswordIncorrect":
            return True
        pending.extend((current.__cause__, current.__context__))
    return False
