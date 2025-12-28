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
     * Analysiert die PDF und gibt die Struktur zurück.
     */
    public PdfFieldAnalyzer.AnalysisResult analyzeTemplate(String customTemplatePath) throws IOException {
        String template = resolveTemplatePath(customTemplatePath);
        return fieldAnalyzer.analyzePdf(template);
    }

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

    private PdfFieldAnalyzer.FieldInfo findFieldInfo(PdfFieldAnalyzer.AnalysisResult analysis, String fieldName) {
        return analysis.getAllFields().stream()
                .filter(f -> f.getFullName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Füllt ein Feld intelligent basierend auf der Analyse und den Request-Daten.
     */
    private boolean fillFieldIntelligently(PDField field, PdfFieldAnalyzer.FieldInfo fieldInfo,
                                           WohngeldAntragRequest request) {
        String name = fieldInfo.getFullName().toLowerCase();
        int personNr = fieldInfo.getPersonNumber();

        // Nur Person 1 (Antragsteller) automatisch ausfüllen
        // Person 2+ muss manuell ergänzt werden
        if (personNr > 1) {
            return false;
        }

        try {
            if (field instanceof PDCheckBox checkbox) {
                return fillCheckbox(checkbox, name, fieldInfo.getCategory(), request);
            } else {
                String value = getValueForField(name, fieldInfo.getCategory(), request);
                if (value != null && !value.isEmpty()) {
                    field.setValue(value);
                    log.debug("Feld '{}' = '{}'", fieldInfo.getFullName(), value);
                    return true;
                }
            }
        } catch (IOException e) {
            log.warn("Fehler bei Feld '{}': {}", fieldInfo.getFullName(), e.getMessage());
        }

        return false;
    }

    /**
     * Füllt eine Checkbox basierend auf Kontext.
     */
    private boolean fillCheckbox(PDCheckBox checkbox, String name, String category,
                                  WohngeldAntragRequest request) throws IOException {
        Boolean shouldCheck = getCheckboxValue(name, category, request);

        if (shouldCheck != null && shouldCheck) {
            checkbox.check();
            log.debug("Checkbox '{}' = checked", name);
            return true;
        }

        return false;
    }

    /**
     * Bestimmt ob eine Checkbox angekreuzt werden soll.
     * Gibt null zurück wenn unklar (dann nicht anfassen!).
     */
    private Boolean getCheckboxValue(String name, String category, WohngeldAntragRequest request) {
        AntragDaten antrag = request.getAntrag();
        Antragsteller a = request.getAntragsteller();

        // === ERSTANTRAG / WEITERLEISTUNG ===
        if (antrag != null) {
            boolean istErstantrag = Boolean.TRUE.equals(antrag.getErstantrag());

            if (matches(name, "erstantrag") && !matches(name, "weiter", "folge")) {
                return istErstantrag;
            }
            if (matches(name, "weiterleistung", "folgeantrag", "weiterbewilligung")) {
                return !istErstantrag;
            }
        }

        // === GESCHLECHT ===
        if (a != null && a.getGeschlecht() != null) {
            String g = a.getGeschlecht().toLowerCase();

            if (matches(name, "männlich", "maennlich") && !matches(name, "weiblich")) {
                return g.equals("maennlich") || g.equals("männlich") || g.equals("m");
            }
            if (matches(name, "weiblich") && !matches(name, "männlich", "maennlich")) {
                return g.equals("weiblich") || g.equals("w");
            }
            if (matches(name, "divers")) {
                return g.equals("divers") || g.equals("d");
            }
        }

        // === FAMILIENSTAND ===
        if (a != null && a.getFamilienstand() != null) {
            String fs = a.getFamilienstand().toLowerCase();

            if (matches(name, "ledig") && !matches(name, "ver", "gesch", "witw", "getr", "partner")) {
                return fs.equals("ledig");
            }
            if (matches(name, "verheiratet") && !matches(name, "nicht", "un")) {
                return fs.equals("verheiratet");
            }
            if (matches(name, "geschieden")) {
                return fs.equals("geschieden");
            }
            if (matches(name, "verwitwet", "witwe", "witwer")) {
                return fs.equals("verwitwet");
            }
            if (matches(name, "getrennt") && matches(name, "lebend")) {
                return fs.contains("getrennt");
            }
            if (matches(name, "lebenspartner", "eingetragen")) {
                return fs.contains("lebenspartner");
            }
        }

        // === ERWERBSSTATUS ===
        if (a != null && a.getErwerbsstatus() != null) {
            String es = a.getErwerbsstatus().toLowerCase();

            if (matches(name, "erwerbstätig", "erwerbstaetig", "beschäftigt", "beschaeftigt")
                && !matches(name, "nicht", "arbeitslos", "selbst")) {
                return es.equals("erwerbstaetig");
            }
            if (matches(name, "arbeitslos", "arbeitssuchend", "arbeitsuchend")) {
                return es.equals("arbeitslos");
            }
            if (matches(name, "rentner", "rentnerin", "rente", "pension", "ruhestand")) {
                return es.equals("rentner");
            }
            if (matches(name, "student", "studentin", "studierend")) {
                return es.equals("student");
            }
            if (matches(name, "schüler", "schueler", "auszubildend", "azubi")) {
                return es.equals("schueler");
            }
            if (matches(name, "selbständig", "selbstständig", "selbstaendig", "selbststaendig", "freiberuf")) {
                return es.equals("selbststaendig");
            }
        }

        // Unbekannte Checkbox -> nicht anfassen!
        return null;
    }

    /**
     * Bestimmt den Wert für ein Textfeld.
     */
    private String getValueForField(String name, String category, WohngeldAntragRequest request) {
        Antragsteller a = request.getAntragsteller();
        Adresse adr = request.getAdresse();
        Wohnung w = request.getWohnung();
        Miete m = request.getMiete();
        Einkommen e = request.getEinkommen();
        Bankverbindung b = request.getBankverbindung();
        AntragDaten antrag = request.getAntrag();
        Haushalt h = request.getHaushalt();

        // === BEHÖRDE ===
        if (category.equals("BEHOERDE") && antrag != null) {
            if (matches(name, "straße", "strasse") && !matches(name, "plz", "ort")) {
                return antrag.getWohngeldbehoerdeStrasse();
            }
            if (matches(name, "plz", "postleitzahl")) {
                return antrag.getWohngeldbehoerdePlz();
            }
            if (matches(name, "ort", "stadt")) {
                return antrag.getWohngeldbehoerdeOrt();
            }
            // Behördenname
            if (!matches(name, "straße", "strasse", "plz", "ort")) {
                return antrag.getWohngeldbehoerde();
            }
        }

        // === ANTRAGSDATEN ===
        if (antrag != null) {
            if (matches(name, "wohngeldnummer", "aktenzeichen", "geschäftszeichen", "az")) {
                return antrag.getWohngeldnummer();
            }
            if (matches(name, "wohngeld ab", "ab wann", "leistung ab", "bewilligung ab", "antrag ab")) {
                return antrag.getWohngeldAb();
            }
            if (matches(name, "antragsdatum", "datum des antrags")) {
                return antrag.getAntragsdatum();
            }
        }

        // === ANTRAGSTELLER ===
        if (a != null) {
            if (matches(name, "familienname", "nachname") && !matches(name, "vorname", "geburtsname")) {
                return a.getNachname();
            }
            if (matches(name, "vorname", "vornamen") && !matches(name, "nachname", "familien")) {
                return a.getVorname();
            }
            if (matches(name, "geburtsdatum", "geb.datum", "geboren am", "geb am")) {
                return a.getGeburtsdatum();
            }
            if (matches(name, "geburtsort", "geb.ort", "geboren in")) {
                return a.getGeburtsort();
            }
            if (matches(name, "staatsangehörigkeit", "staatsangehoerigkeit", "nationalität", "nation")) {
                return a.getStaatsangehoerigkeit();
            }
            if (matches(name, "telefon", "tel.", "rufnummer", "mobil", "handy")) {
                return a.getTelefon();
            }
            if (matches(name, "email", "e-mail", "mail")) {
                return a.getEmail();
            }
        }

        // === ADRESSE (Wohnung, nicht Behörde!) ===
        if (adr != null && !category.equals("BEHOERDE")) {
            if (matches(name, "straße", "strasse", "str.") && !matches(name, "hausnr", "nummer", "behörde", "behoerde")) {
                return adr.getStrasse();
            }
            if (matches(name, "hausnummer", "hausnr", "haus-nr", "hnr", "nr.") && !matches(name, "wohnung")) {
                return adr.getHausnummer();
            }
            if (matches(name, "postleitzahl", "plz") && !matches(name, "behörde", "behoerde")) {
                return adr.getPlz();
            }
            if (matches(name, "wohnort", "ort", "stadt", "gemeinde") &&
                !matches(name, "geburts", "behörde", "behoerde")) {
                return adr.getOrt();
            }
            if (matches(name, "bundesland")) {
                return adr.getBundesland();
            }
        }

        // === WOHNUNG ===
        if (w != null) {
            if (matches(name, "einzug", "bezug", "eingezogen", "wohnt seit", "seit wann")) {
                return w.getEinzugsdatum();
            }
            if (matches(name, "wohnfläche", "wohnflaeche", "qm", "quadratmeter", "fläche", "flaeche", "größe", "groesse")) {
                return w.getWohnflaecheQm() != null ? String.format("%.0f", w.getWohnflaecheQm()) : null;
            }
            if (matches(name, "zimmer", "räume", "raeume") && matches(name, "anzahl", "zahl")) {
                return w.getAnzahlRaeume() != null ? String.valueOf(w.getAnzahlRaeume()) : null;
            }
            if (matches(name, "baujahr", "erbaut", "errichtet", "fertiggestellt")) {
                return w.getBaujahr();
            }
            if (matches(name, "vermieter", "eigentümer", "eigentuemer", "hausverwaltung", "vermieterin")) {
                return w.getVermieterName();
            }
        }

        // === MIETE ===
        if (m != null) {
            if (matches(name, "kaltmiete", "grundmiete", "miete ohne", "nettomiete") &&
                !matches(name, "warm", "brutto", "gesamt")) {
                return formatCurrency(m.getKaltmiete());
            }
            if (matches(name, "nebenkosten", "betriebskosten", "nk", "umlagen")) {
                return formatCurrency(m.getNebenkosten());
            }
            if (matches(name, "heizkosten", "heizung") && !matches(name, "art", "typ")) {
                return formatCurrency(m.getHeizkosten());
            }
            if (matches(name, "warmwasser", "ww")) {
                return formatCurrency(m.getWarmwasser());
            }
            if (matches(name, "gesamtmiete", "warmmiete", "bruttomiete", "miete gesamt", "insgesamt", "summe")) {
                return formatCurrency(m.getGesamtmiete());
            }
        }

        // === EINKOMMEN ===
        if (e != null) {
            if (matches(name, "brutto") && matches(name, "einkommen", "verdienst", "gehalt", "lohn", "einnahmen")) {
                return formatCurrency(e.getBruttoeinkommenMonatlich());
            }
            if (matches(name, "netto") && matches(name, "einkommen", "verdienst", "gehalt", "lohn", "einnahmen")) {
                return formatCurrency(e.getNettoeinkommenMonatlich());
            }
            if (matches(name, "kindergeld") && e.getKindergeld() != null && e.getKindergeld() > 0) {
                return formatCurrency(e.getKindergeld());
            }
            if (matches(name, "unterhalt") && e.getUnterhalt() != null && e.getUnterhalt() > 0) {
                return formatCurrency(e.getUnterhalt());
            }
        }

        // === BANKVERBINDUNG ===
        if (b != null) {
            if (matches(name, "iban")) {
                return b.getIban();
            }
            if (matches(name, "bic", "swift")) {
                return b.getBic();
            }
            if (matches(name, "bank", "kreditinstitut", "geldinstitut", "sparkasse")) {
                return b.getBank();
            }
            if (matches(name, "kontoinhaber", "kontoinhaberin")) {
                return b.getKontoinhaber();
            }
        }

        // === HAUSHALT ===
        if (h != null) {
            if (matches(name, "anzahl") && matches(name, "person", "haushalt", "mitglieder")) {
                return String.valueOf(h.getAnzahlPersonen());
            }
        }

        return null;
    }

    /**
     * Prüft ob der Name einen der Begriffe enthält.
     */
    private boolean matches(String name, String... terms) {
        for (String term : terms) {
            if (name.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String formatCurrency(Double value) {
        if (value == null) return null;
        // Deutsches Format mit Komma
        return String.format("%.2f", value).replace(".", ",");
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
