package com.example.pdfanalyzer.exception;

public class PdfTooLargeException extends PdfAnalysisException {
    public PdfTooLargeException() {
        super("The PDF exceeds the maximum supported size of 10 MB.");
    }
}
