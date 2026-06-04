package com.example.pdfanalyzer.exception;

public class InvalidUrlFormatException extends PdfAnalysisException {
    public InvalidUrlFormatException() {
        super("Please enter a valid HTTP or HTTPS URL.");
    }
}
