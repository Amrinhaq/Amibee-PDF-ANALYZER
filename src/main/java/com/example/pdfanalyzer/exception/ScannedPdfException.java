package com.example.pdfanalyzer.exception;

public class ScannedPdfException extends PdfAnalysisException {
    public ScannedPdfException() {
        super("No readable text could be extracted from this PDF. It may contain scanned images only.");
    }
}
