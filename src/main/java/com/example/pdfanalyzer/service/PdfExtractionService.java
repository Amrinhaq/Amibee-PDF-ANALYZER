package com.example.pdfanalyzer.service;

import com.example.pdfanalyzer.exception.EmptyPdfException;
import com.example.pdfanalyzer.exception.PdfAnalysisException;
import com.example.pdfanalyzer.exception.ScannedPdfException;
import com.example.pdfanalyzer.exception.UnexpectedErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

/**
 * Service to extract text from a PDF document using Apache PDFBox.
 * Map extraction states to specific user-friendly exception structures.
 */
@Service
@Slf4j
public class PdfExtractionService {

    private static final int MAX_PAGES_TO_PROCESS = 30;
    private static final int MAX_CHARACTER_LIMIT = 50000;

    /**
     * Extracts text from PDF bytes.
     * Throws EmptyPdfException or ScannedPdfException depending on text extraction yields.
     */
    public String extractText(byte[] pdfBytes) {
        log.info("Extracting text from PDF bytes...");

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            log.info("PDF loaded successfully. Total pages: {}", totalPages);

            if (totalPages == 0) {
                throw new EmptyPdfException();
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            int endPage = Math.min(totalPages, MAX_PAGES_TO_PROCESS);
            stripper.setEndPage(endPage);
            
            String extractedText = stripper.getText(document);

            // Check if text is completely null or empty
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new EmptyPdfException();
            }

            // Normalize spacing
            extractedText = extractedText.replaceAll("\\s+", " ").trim();

            // Verify if there is any alphanumeric content. 
            // If the PDF has only whitespace, punctuation, or control characters, it's scanned or image-based.
            if (!extractedText.matches(".*[a-zA-Z0-9].*")) {
                log.warn("Text was extracted but contains no alphanumeric characters. PDF is likely scanned.");
                throw new ScannedPdfException();
            }

            // Enforce character limit to keep LLM context light
            if (extractedText.length() > MAX_CHARACTER_LIMIT) {
                log.info("Extracted text truncated from {} to {} characters.", extractedText.length(), MAX_CHARACTER_LIMIT);
                extractedText = extractedText.substring(0, MAX_CHARACTER_LIMIT);
            }

            return extractedText;

        } catch (PdfAnalysisException e) {
            // Rethrow our domain exceptions directly
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse PDF document structures: ", e);
            throw new UnexpectedErrorException("Failed to parse PDF", e);
        }
    }
}
