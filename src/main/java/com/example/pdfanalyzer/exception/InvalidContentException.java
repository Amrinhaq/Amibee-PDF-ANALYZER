package com.example.pdfanalyzer.exception;

public class InvalidContentException extends PdfAnalysisException {
    
    public InvalidContentException() {
        super("The provided URL does not point to a PDF document. Please enter a valid PDF URL.");
    }

    public InvalidContentException(String customMessage) {

        super(customMessage);
    }
}
