package com.example.pdfanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for capturing the PDF analysis request from the client.
 * Using Lombok annotations to automatically generate boilerplates:
 * - @Data: generates getter/setter, toString, equals, and hashCode.
 * - @NoArgsConstructor: generates a default no-argument constructor.
 * - @AllArgsConstructor: generates a constructor for all fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfAnalysisRequest {
    
    /**
     * The publicly accessible URL pointing to the PDF document.
     */
    private String url;
}
