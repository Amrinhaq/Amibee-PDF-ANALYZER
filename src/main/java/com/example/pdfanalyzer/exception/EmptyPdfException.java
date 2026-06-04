package com.example.pdfanalyzer.exception;

public class EmptyPdfException extends PdfAnalysisException {
    public EmptyPdfException() {
        super("The PDF appears to be empty or contains no readable content.");
    }
}
