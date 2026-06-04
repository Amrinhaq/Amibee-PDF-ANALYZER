package com.example.pdfanalyzer.exception;

public class RedirectedUrlException extends PdfAnalysisException {
    public RedirectedUrlException() {
        super("This URL redirects to another page and does not directly point to a PDF file. Please provide a direct PDF URL.");
    }
}
