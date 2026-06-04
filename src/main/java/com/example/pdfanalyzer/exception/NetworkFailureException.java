package com.example.pdfanalyzer.exception;

public class NetworkFailureException extends PdfAnalysisException {
    public NetworkFailureException(String technicalDetails, Throwable cause) {
        super("Unable to access the document. Please check the URL and your network connection.", cause);
    }
}
