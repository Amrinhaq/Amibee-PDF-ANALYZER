package com.example.pdfanalyzer.exception;

/**
 * Base exception for all PDF analysis domain errors.
 * Ensures we can catch any business error and map it to a user-friendly message.
 */
public abstract class PdfAnalysisException extends RuntimeException {
    
    public PdfAnalysisException(String message) {
        super(message);
    }

    public PdfAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
