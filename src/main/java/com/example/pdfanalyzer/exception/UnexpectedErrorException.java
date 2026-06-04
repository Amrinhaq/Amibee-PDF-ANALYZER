package com.example.pdfanalyzer.exception;

public class UnexpectedErrorException extends PdfAnalysisException {
    public UnexpectedErrorException(String message, Throwable cause) {
        super("An unexpected error occurred while processing the document. Please try again.", cause);
    }
}
