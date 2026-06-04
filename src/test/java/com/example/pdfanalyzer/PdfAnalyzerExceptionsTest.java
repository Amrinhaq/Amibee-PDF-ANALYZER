package com.example.pdfanalyzer;

import com.example.pdfanalyzer.exception.*;
import com.example.pdfanalyzer.service.GeminiService;
import com.example.pdfanalyzer.service.PdfDownloadService;
import com.example.pdfanalyzer.service.PdfExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfAnalyzerExceptionsTest {

    private PdfDownloadService downloadService;
    private PdfExtractionService extractionService;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        downloadService = new PdfDownloadService();
        extractionService = new PdfExtractionService();
        geminiService = new GeminiService(new ObjectMapper());
    }

    // --- PDF DOWNLOAD SERVICE TESTS (SSRF & PROTOCOL VALIDATION) ---

    @Test
    void testInvalidProtocol_ShouldThrow_InvalidUrlFormatException() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            downloadService.downloadPdf("ftp://example.com/document.pdf");
        });
    }

    @Test
    void testLocalHostBlocked_ShouldThrow_InvalidUrlFormatException() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            downloadService.downloadPdf("http://localhost:8080/document.pdf");
        });
    }

    @Test
    void testLoopbackIpBlocked_ShouldThrow_InvalidUrlFormatException() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            downloadService.downloadPdf("http://127.0.0.1/document.pdf");
        });
    }

    @Test
    void testPrivateIpBlocked_ShouldThrow_InvalidUrlFormatException() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            downloadService.downloadPdf("http://192.168.1.50/document.pdf");
        });
    }

    @Test
    void testMalformedUrl_ShouldThrow_InvalidUrlFormatException() {
        assertThrows(InvalidUrlFormatException.class, () -> {
            downloadService.downloadPdf("not_a_url");
        });
    }

    // --- PDF EXTRACTION SERVICE TESTS (EMPTY & SCANNED DOCUMENTS) ---

    @Test
    void testEmptyPdfWithNoPages_ShouldThrow_EmptyPdfException() throws IOException {
        byte[] emptyPdfBytes;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.save(out);
            emptyPdfBytes = out.toByteArray();
        }

        assertThrows(EmptyPdfException.class, () -> {
            extractionService.extractText(emptyPdfBytes);
        });
    }

    @Test
    void testScannedPdfWithBlankPage_ShouldThrow_EmptyPdfExceptionOrScannedPdfException() throws IOException {
        byte[] scannedPdfBytes;
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Add a blank page with no text content
            doc.addPage(new PDPage());
            doc.save(out);
            scannedPdfBytes = out.toByteArray();
        }

        // When a page has absolutely no text elements, PDFTextStripper yields empty/null string.
        // This triggers EmptyPdfException.
        assertThrows(EmptyPdfException.class, () -> {
            extractionService.extractText(scannedPdfBytes);
        });
    }

    // --- GEMINI SERVICE TESTS ---

    @Test
    void testGeminiApiKeyUnconfigured_ShouldThrow_IllegalArgumentException() {
        // With an empty key or the generic placeholder key, it must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            geminiService.analyzeText("Sample PDF text to analyze");
        });
    }
}
