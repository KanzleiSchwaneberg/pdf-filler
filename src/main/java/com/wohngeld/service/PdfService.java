package com.wohngeld.service;

import com.wohngeld.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfService {

    private static final String TEMPLATE_CLASSPATH = "templates/Antrag-auf-Mietzuschuss.pdf";

    private final PdfFieldAnalyzer fieldAnalyzer;
    private final PdfFieldMapper fieldMapper;

    @Value("${wohngeld.output.directory:output}")
    private String outputDirectory;

    /**
     * Gibt alle Feldnamen der PDF zurück (nur die rohen Namen aus dem PDF).
     */
    public List<String> getFormFields(String pdfPath) throws IOException {
        String template = resolveTemplatePath(pdfPath);
        PdfFieldAnalyzer.AnalysisResult analysis = fieldAnalyzer.analyzePdf(template);

        List<String> result = new ArrayList<>();
        for (PdfFieldAnalyzer.FieldInfo field : analysis.getAllFields()) {
            result.add(field.getFullName());
        }
        return result;
    }

    /**
     * Füllt die PDF mit den Antragsdaten aus.
     * Uses direct field mapping for reliable PDF filling.
     */
    public PdfResult fillPdf(WohngeldAntragRequest request, String customTemplatePath) throws IOException {
        String template = resolveTemplatePath(customTemplatePath);
        Path outputPath = generateOutputPath(request.getAntragsteller().getNachname());

        Files.createDirectories(outputPath.getParent());

        int fieldsFound = 0;
        int fieldsFilled = 0;

        // Get the field mapping from request data
        Map<String, Object> fieldMapping = fieldMapper.createFieldMapping(request);
        log.info("Field mapping created with {} entries", fieldMapping.size());

        try (PDDocument document = Loader.loadPDF(new File(template))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Count all fields
                for (PDField ignored : acroForm.getFieldTree()) {
                    fieldsFound++;
                }

                // Fill each field using direct mapping
                for (PDField field : acroForm.getFieldTree()) {
                    String fieldName = field.getFullyQualifiedName();

                    // Try to find a mapping for this field (with various encoding variations)
                    Object value = findMappingValue(fieldMapping, fieldName);

                    if (value != null) {
                        boolean filled = fillField(field, value);
                        if (filled) {
                            fieldsFilled++;
                            log.debug("Filled field '{}' = '{}'", fieldName, value);
                        }
                    }
                }
            }

            document.save(outputPath.toFile());
        }

        log.info("PDF erstellt: {} (Felder: {}, ausgefüllt: {})",
                outputPath.getFileName(), fieldsFound, fieldsFilled);

        return PdfResult.builder()
                .outputPath(outputPath.toString())
                .filename(outputPath.getFileName().toString())
                .fieldsFound(fieldsFound)
                .fieldsFilled(fieldsFilled)
                .build();
    }

    /**
     * Finds a mapping value for a field, handling encoding variations.
     */
    private Object findMappingValue(Map<String, Object> fieldMapping, String fieldName) {
        // Direct match
        if (fieldMapping.containsKey(fieldName)) {
            return fieldMapping.get(fieldName);
        }

        // Try normalized versions (handle encoding issues with German characters)
        String normalized = normalizeFieldName(fieldName);
        for (Map.Entry<String, Object> entry : fieldMapping.entrySet()) {
            if (normalizeFieldName(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Normalizes a field name for comparison (handles encoding issues).
     */
    private String normalizeFieldName(String name) {
        return name
            .replace("ä", "ae").replace("Ä", "Ae")
            .replace("ö", "oe").replace("Ö", "Oe")
            .replace("ü", "ue").replace("Ü", "Ue")
            .replace("ß", "ss")
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")  // More variations
            .replace("�?", "ß").replace("��", "ä")  // Common encoding errors
            .replaceAll("[^a-zA-Z0-9_-]", "");  // Remove special chars
    }

    /**
     * Fills a single field with the given value.
     */
    private boolean fillField(PDField field, Object value) {
        try {
            if (field instanceof PDCheckBox checkbox) {
                if (value instanceof Boolean bool && bool) {
                    checkbox.check();
                    return true;
                } else if ("true".equalsIgnoreCase(String.valueOf(value))) {
                    checkbox.check();
                    return true;
                }
            } else if (value != null) {
                String strValue = String.valueOf(value);
                if (!strValue.isEmpty() && !"null".equals(strValue)) {
                    field.setValue(strValue);
                    return true;
                }
            }
        } catch (IOException e) {
            log.warn("Error filling field '{}': {}", field.getFullyQualifiedName(), e.getMessage());
        }
        return false;
    }

    /**
     * Gibt den Standard-Pfad zur PDF-Vorlage zurück.
     */
    public String getDefaultTemplatePath() {
        try {
            return resolveTemplatePath(null);
        } catch (IOException e) {
            return TEMPLATE_CLASSPATH;
        }
    }

    /**
     * Resolves the template path. If no custom path is provided, extracts the
     * template from classpath to a temp file.
     */
    private String resolveTemplatePath(String customPath) throws IOException {
        if (customPath != null && !customPath.isBlank()) {
            File customFile = new File(customPath);
            if (!customFile.exists()) {
                throw new IOException("PDF-Vorlage nicht gefunden: " + customPath);
            }
            return customPath;
        }

        // Extract template from classpath to temp file
        ClassPathResource resource = new ClassPathResource(TEMPLATE_CLASSPATH);
        if (!resource.exists()) {
            throw new IOException("PDF-Vorlage nicht im Classpath gefunden: " + TEMPLATE_CLASSPATH);
        }

        Path tempFile = Files.createTempFile("wohngeld-template-", ".pdf");
        tempFile.toFile().deleteOnExit();

        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("Template extracted to: {}", tempFile);
        return tempFile.toString();
    }

    private Path generateOutputPath(String nachname) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("wohngeldantrag_%s_%s.pdf", nachname, timestamp);
        return Paths.get(outputDirectory, filename);
    }
}
