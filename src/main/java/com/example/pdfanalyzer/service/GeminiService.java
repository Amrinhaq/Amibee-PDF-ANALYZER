package com.example.pdfanalyzer.service;

import com.example.pdfanalyzer.dto.PdfAnalysisResponse;
import com.example.pdfanalyzer.exception.GeminiApiException;
import com.example.pdfanalyzer.exception.PdfAnalysisException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service to interface with Google Gemini Generative AI API.
 * Uses structured JSON schema response configurations.
 */
@Service
@Slf4j
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String apiUrl;

    public GeminiService(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends extracted text to Gemini API for analysis.
     * Throws GeminiApiException if any connection, auth, or response errors occur.
     */
    public PdfAnalysisResponse analyzeText(String extractedText) {
        log.info("Sending text to Gemini API for metadata extraction...");

        if (apiKey == null || apiKey.trim().isEmpty() || "YOUR_GEMINI_API_KEY".equals(apiKey)) {
            throw new IllegalArgumentException("Google Gemini API Key is not configured. Please add a valid API key to application.properties.");
        }

        try {
            // Build the system prompt and instructions
            String prompt = "You are a professional PDF Analyzer and research assistant. "
                    + "Analyze the following text extracted from a PDF document and extract the requested metadata. "
                    + "Provide the document title, authors, document type, a concise summary, and the key takeaway. "
                    + "Document Text:\n\n" + extractedText;

            // Assemble request structure using helper classes
            GeminiRequest requestPayload = new GeminiRequest(
                    Collections.singletonList(new Content(Collections.singletonList(new Part(prompt)))),
                    new GenerationConfig("application/json", new ResponseSchema(
                            "OBJECT",
                            Map.of(
                                    "documentType", Map.of("type", "STRING", "description", "Category of document: Research Paper, Business Report, Invoice, Resume, User Manual, etc."),
                                    "title", Map.of("type", "STRING", "description", "The official title of the document"),
                                    "authors", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"), "description", "List of authors or contributors"),
                                    "summary", Map.of("type", "STRING", "description", "Concise executive summary"),
                                    "keyTakeaway", Map.of("type", "STRING", "description", "The main point or takeaway")
                            ),
                            List.of("documentType", "title", "authors", "summary", "keyTakeaway")
                    ))
            );

            // Execute POST request
            String responseJson = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            // Parse response structure
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isMissingNode() || candidatesNode.isEmpty()) {
                throw new RuntimeException("Gemini API returned an empty or error response: " + responseJson);
            }

            // Gemini API response structure: candidates[0].content.parts[0].text
            String structuredResponseText = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            log.info("Successfully received structured response: {}", structuredResponseText);

            // Deserialize the response directly into the target DTO
            return objectMapper.readValue(structuredResponseText, PdfAnalysisResponse.class);

        } catch (PdfAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query Gemini API or parse results: ", e);
            throw new GeminiApiException("Gemini integration failure", e);
        }
    }

    // --- INNER CLASSES FOR GEMINI API JSON PAYLOAD MAPPING ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GeminiRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class GenerationConfig {
        private String responseMimeType;
        private ResponseSchema responseSchema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ResponseSchema {
        private String type;
        private Map<String, Object> properties;
        private List<String> required;
    }
}
