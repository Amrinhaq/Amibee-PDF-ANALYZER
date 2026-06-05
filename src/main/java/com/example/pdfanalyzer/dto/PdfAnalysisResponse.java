package com.example.pdfanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfAnalysisResponse {

    /**
     * Categorized document type (e.g., Research Paper, Business Report, Invoice, Resume, etc.).
     */
    private String documentType;

    /**
     * The extracted title of the PDF document.
     */
    private String title;

    /**
     * List of authors or contributors identified in the document.
     */
    private List<String> authors;

    /**
     * A concise executive summary of the document contents.
     */
    private String summary;

    /**
     * The single most important takeaway, finding, or conclusion from the document.
     */
    private String keyTakeaway;
}
