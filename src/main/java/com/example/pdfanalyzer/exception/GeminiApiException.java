package com.example.pdfanalyzer.exception;

public class GeminiApiException extends PdfAnalysisException {
    public GeminiApiException(String technicalDetails, Throwable cause) {
        super("Document analysis is temporarily unavailable. Please try again later.", cause);
    }
}
