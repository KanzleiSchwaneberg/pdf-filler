package com.wohngeld.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Hilfsprogramm zum Analysieren der PDF-Formularfelder.
 * Führe diese Klasse aus, um alle Feldnamen zu sehen.
 */
public class PdfFieldAnalyzer {

    public static void main(String[] args) throws IOException {
        String pdfPath = args.length > 0 ? args[0] :
                System.getProperty("user.home") + "/Downloads/Antrag-auf-Mietzuschuss.pdf";

        System.out.println("Analysiere PDF: " + pdfPath);
        System.out.println("=".repeat(80));

        List<FieldInfo> fields = analyzeFields(pdfPath);

        // Nach Kategorien gruppieren
        System.out.println("\n=== ALLE FORMULARFELDER ===\n");

        for (FieldInfo field : fields) {
            System.out.printf("%-60s | %-15s | %s%n",
                    field.name,
                    field.type,
                    field.value != null ? field.value : "");
        }

        System.out.println("\n=".repeat(80));
        System.out.println("Gesamt: " + fields.size() + " Felder");

        // Speichere in Datei
        Path outputPath = Paths.get("pdf_fields.txt");
        StringBuilder sb = new StringBuilder();
        for (FieldInfo field : fields) {
            sb.append(field.name).append("\n");
        }
        Files.writeString(outputPath, sb.toString());
        System.out.println("\nFeldnamen gespeichert in: " + outputPath.toAbsolutePath());
    }

    public static List<FieldInfo> analyzeFields(String pdfPath) throws IOException {
        List<FieldInfo> fields = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                for (PDField field : acroForm.getFieldTree()) {
                    fields.add(new FieldInfo(
                            field.getFullyQualifiedName(),
                            field.getFieldType(),
                            field.getValueAsString()
                    ));
                }
            }
        }

        return fields;
    }

    public record FieldInfo(String name, String type, String value) {}
}
