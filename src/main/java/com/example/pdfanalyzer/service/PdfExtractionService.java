package com.example.pdfanalyzer.service;

import com.example.pdfanalyzer.exception.EmptyPdfException;
import com.example.pdfanalyzer.exception.InvalidContentException;
import com.example.pdfanalyzer.exception.PdfAnalysisException;
import com.example.pdfanalyzer.exception.ScannedPdfException;
import com.example.pdfanalyzer.exception.UnexpectedErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
     * Accurately distinguishes Empty vs. Scanned PDFs using page resources.
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

            // Clean and normalize spacing
            String cleanText = extractedText != null ? extractedText.replaceAll("\\s+", " ").trim() : "";

            // Check if text is completely null or empty
            if (cleanText.isEmpty()) {
                if (hasImages(document)) {
                    log.warn("Document has pages but yielded zero text, and contains image resources. PDF is scanned.");
                    throw new ScannedPdfException();
                } else {
                    log.warn("Document has pages but contains neither text nor image resources. PDF is empty.");
                    throw new EmptyPdfException();
                }
            }

            // Verify if there is any alphanumeric content. 
            // (If the PDF has only formatting spaces or punctuation marks)
            if (!cleanText.matches(".*[a-zA-Z0-9].*")) {
                if (hasImages(document)) {
                    log.warn("Text contains no alphanumeric characters, but page has image resources. PDF is scanned.");
                    throw new ScannedPdfException();
                } else {
                    log.warn("Text contains no alphanumeric characters, and contains no image resources. PDF is empty.");
                    throw new EmptyPdfException();
                }
            }

            // Enforce character limit to keep LLM context light
            if (cleanText.length() > MAX_CHARACTER_LIMIT) {
                log.info("Extracted text truncated from {} to {} characters.", cleanText.length(), MAX_CHARACTER_LIMIT);
                cleanText = cleanText.substring(0, MAX_CHARACTER_LIMIT);
            }

            return cleanText;

        } catch (PdfAnalysisException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to load PDF document: invalid format or signature.", e);
            throw new InvalidContentException();
        } catch (Exception e) {
            log.error("Failed to parse PDF document structures: ", e);
            throw new UnexpectedErrorException("Failed to parse PDF", e);
        }
    }

    /**
     * Inspects the PDF's internal resources directory to check for raster images.
     * This is extremely fast because it reads metadata dictionaries without loading image bytes into memory.
     */
    private boolean hasImages(PDDocument document) {
        try {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (COSName name : resources.getXObjectNames()) {
                        if (resources.isImageXObject(name)) {
                            return true; // Found an image object in resources
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to inspect PDF page resources for images", e);
        }
        return false;
    }
}
