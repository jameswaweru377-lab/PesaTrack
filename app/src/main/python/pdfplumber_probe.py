"""Build-only proof that Android can bundle the same pdfplumber release.

No file IO, logging, PDF access, network calls or ledger interaction.
Full source and dependency-version equivalence to the user's laptop
must be established before any production integration.
"""


def import_probe():
    import pdfplumber
    from pdfminer import __version__ as pdfminer_version

    return {
        "available": True,
        "pdfplumber": getattr(pdfplumber, "__version__", "unknown"),
        "pdfminer": str(pdfminer_version),
    }
