package com.example.pdfanalyzer.controller;

import com.example.pdfanalyzer.dto.PdfAnalysisRequest;
import com.example.pdfanalyzer.dto.PdfAnalysisResponse;
import com.example.pdfanalyzer.exception.PdfAnalysisException;
import com.example.pdfanalyzer.service.GeminiService;
import com.example.pdfanalyzer.service.PdfDownloadService;
import com.example.pdfanalyzer.service.PdfExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URI;

/**
 * Controller class for the PDF Analyzer web application following MVC architecture.
 * Implements robust custom exception handling to protect system metadata.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PdfAnalyzerController {

    private final PdfDownloadService downloadService;
    private final PdfExtractionService extractionService;
    private final GeminiService geminiService;

    @GetMapping("/")
    public String showHomepage(Model model) {
        model.addAttribute("analysisRequest", new PdfAnalysisRequest());
        return "index";
    }

    @PostMapping("/analyze")
    public String analyzePdf(@ModelAttribute("analysisRequest") PdfAnalysisRequest analysisRequest, Model model) {
        log.info("Received request to analyze PDF URL: {}", analysisRequest.getUrl());

        String urlString = analysisRequest.getUrl();
        if (urlString == null || urlString.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a valid HTTP or HTTPS URL.");
            return "index";
        }

        try {
            // Basic URL syntax check
            URI.create(urlString).toURL();
        } catch (Exception e) {
            log.error("Invalid URL syntax format: {}", urlString, e);
            model.addAttribute("error", "Please enter a valid HTTP or HTTPS URL.");
            return "index";
        }

        try {
            // 1. Download PDF bytes (safely checks SSRF, Content-Length, Content-Type, and redirects)
            byte[] pdfBytes = downloadService.downloadPdf(urlString);
            log.info("Successfully downloaded PDF. Size: {} bytes", pdfBytes.length);

            // 2. Extract text from PDF (safely checks page limits, empty structures, and scanned PDFs)
            String extractedText = extractionService.extractText(pdfBytes);
            log.info("Successfully extracted text. Characters: {}", extractedText.length());

            // 3. Send text to Gemini API for analysis (safely catches API failures)
            PdfAnalysisResponse analysisResponse = geminiService.analyzeText(extractedText);
            log.info("Successfully analyzed PDF with Google Gemini API.");

            // 4. Return results to the model
            model.addAttribute("result", analysisResponse);

        } catch (PdfAnalysisException e) {
            // Log full technical stack trace details locally (NOT exposed to UI)
            log.error("Technical exception encountered during PDF analysis: ", e);
            
            // Map the user-friendly message directly to the Thymeleaf template error display
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            // Fallback for general uncaught technical errors
            log.error("Unexpected runtime exception during PDF analysis: ", e);
            model.addAttribute("error", "An unexpected error occurred while processing the document. Please try again.");
        }

        return "index";
    }
}
