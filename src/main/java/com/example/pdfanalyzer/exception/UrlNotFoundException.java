package com.example.pdfanalyzer.exception;

public class UrlNotFoundException extends PdfAnalysisException {
    public UrlNotFoundException() {
        super("The PDF could not be found at the provided URL. Please verify the link and try again.");
    }
}
