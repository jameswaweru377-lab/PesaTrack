"""Local synthetic extraction benchmark; never supply a real statement.

Optional desktop dependencies: reportlab, pypdf, pdfplumber. These are NOT
application dependencies. Generates invented ruled and encrypted borderless
26-page/881-row documents. Accuracy is exact cells, not receipt marker count.
"""
import argparse
import io
import json
import re
import time
from pathlib import Path


def generate(target, ruled):
    from reportlab.pdfgen import canvas
    from pypdf import PdfReader, PdfWriter

    stream = io.BytesIO()
    pdf = canvas.Canvas(stream, pagesize=(612, 792), pageCompression=1)
    expected = []
    labels = ['Date', 'Description', 'Debit', 'Credit', 'Balance']
    borders = [30, 132, 354, 429, 504, 582]
    x = [36, 138, 360, 435, 510]
    for page in range(26):
        order = [0, 1, 2, 3, 4] if page % 2 == 0 else [0, 1, 3, 2, 4]
        pdf.setFont('Helvetica', 8)
        pdf.drawString(36, 760, 'Invented benchmark statement - no real account')
        for column, role in enumerate(order):
            pdf.drawString(x[column], 720, labels[role])
        n = 31 if page == 25 else 34
        if ruled:
            for boundary in borders:
                pdf.line(boundary, 732, boundary, 714 - n * 18)
            for row in range(n + 2):
                y = 732 - row * 18
                pdf.line(borders[0], y, borders[-1], y)
        for row in range(n):
            index = len(expected)
            day = f'2026-01-{index % 28 + 1:02d}'
            cells = [day, f'Invented shop {index}', '10.00', '-', f'{10000-index*10:.2f}']
            expected.append(cells[:])
            y = 706 - row * 18
            for column, role in enumerate(order):
                pdf.drawString(x[column], y, cells[role])
            if row % 7 == 0:
                pdf.drawString(x[1], y - 8, 'from 07***12345')
                expected[-1][1] += ' from 07***12345'
        pdf.showPage()
    pdf.save()
    if ruled:
        target.write_bytes(stream.getvalue())
    else:
        writer = PdfWriter()
        writer.clone_document_from_reader(PdfReader(io.BytesIO(stream.getvalue())))
        writer.encrypt('invented-benchmark-password', algorithm='AES-256')
        with target.open('wb') as output:
            writer.write(output)
    return expected


def measure(path, expected, ruled, configured=False):
    import pdfplumber

    start = time.perf_counter()
    reconstructed = []
    settings = {} if ruled else {'vertical_strategy': 'text', 'horizontal_strategy': 'text',
                                'min_words_vertical': 3, 'intersection_tolerance': 5}
    if configured:
        settings = {'vertical_strategy': 'explicit', 'horizontal_strategy': 'text',
                    'explicit_vertical_lines': [30, 132, 354, 429, 504, 582],
                    'intersection_tolerance': 5, 'intersection_x_tolerance': 60}
    with pdfplumber.open(path, password=None if ruled else 'invented-benchmark-password') as document:
        for page in document.pages:
            for table in page.extract_tables(settings):
                mapping = None
                current = None
                for row in table:
                    normalized = [' '.join((cell or '').split()) for cell in row]
                    if set(normalized) == {'Date', 'Description', 'Debit', 'Credit', 'Balance'}:
                        mapping = [normalized.index(label) for label in ['Date', 'Description', 'Debit', 'Credit', 'Balance']]
                    elif mapping is not None and len(row) == 5:
                        cells = [normalized[i] for i in mapping]
                        if re.fullmatch(r'2026-01-\d{2}', cells[0]):
                            current = cells
                            reconstructed.append(cells)
                        elif current is not None and cells[1] and all(not cells[i] for i in [0, 2, 3, 4]):
                            current[1] += ' ' + cells[1]
    elapsed = time.perf_counter() - start
    exact = sum(a == b for a, b in zip(reconstructed, expected))
    return {'engine': 'pdfplumber', 'fixture': path.name,
            'configuration': 'known column boundaries' if configured else 'lines' if ruled else 'text discovery',
            'pages': 26, 'expected_rows': 881,
            'reconstructed_rows': len(reconstructed), 'exact_rows_in_source_order': exact,
            'elapsed_seconds': round(elapsed, 3), 'complete': reconstructed == expected,
            'scope': 'Invented desktop fixtures only; not Android performance or real-statement acceptance.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    results = []
    for ruled in (True, False):
        path = args.out / ('invented-ruled.pdf' if ruled else 'invented-borderless-encrypted.pdf')
        expected = generate(path, ruled)
        results.append(measure(path, expected, ruled))
        if not ruled:
            results.append(measure(path, expected, ruled, configured=True))
    (args.out / 'benchmark.json').write_text(json.dumps(results, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(results, indent=2))


if __name__ == '__main__':
    main()
