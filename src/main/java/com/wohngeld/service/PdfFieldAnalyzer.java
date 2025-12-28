// java
package com.wohngeld.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analysiert PDF-Formularfelder und erstellt ein intelligentes Mapping.
 * Arbeitet wie ein Detektiv: Untersucht Feldnamen, Struktur und Kontext.
 */
@Service
@Slf4j
public class PdfFieldAnalyzer {

    @Data
    public static class FieldInfo {
        private String fullName;
        private String shortName;
        private String type;
        private String category;
        private int sectionNumber;
        private int personNumber;
        // Feld umbenannt von `isCheckbox` auf `checkbox` damit Lombok die erwarteten Methoden
        // `isCheckbox()` und `setCheckbox(boolean)` erzeugt.
        private boolean checkbox;
        private String currentValue;
    }

    @Data
    public static class AnalysisResult {
        private List<FieldInfo> allFields = new ArrayList<>();
        private Map<String, List<FieldInfo>> byCategory = new HashMap<>();
        private Map<Integer, List<FieldInfo>> bySection = new HashMap<>();
        private Map<String, String> recommendedMapping = new LinkedHashMap<>();
    }

    /**
     * Analysiert die PDF und gibt eine strukturierte Analyse zurück.
     */
    public AnalysisResult analyzePdf(String pdfPath) throws IOException {
        AnalysisResult result = new AnalysisResult();

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm == null) {
                log.warn("Keine AcroForm gefunden");
                return result;
            }

            // Alle Felder sammeln und analysieren
            for (PDField field : acroForm.getFieldTree()) {
                FieldInfo info = analyzeField(field);
                result.getAllFields().add(info);

                // Nach Kategorie gruppieren
                result.getByCategory()
                        .computeIfAbsent(info.getCategory(), k -> new ArrayList<>())
                        .add(info);

                // Nach Abschnitt gruppieren
                if (info.getSectionNumber() > 0) {
                    result.getBySection()
                            .computeIfAbsent(info.getSectionNumber(), k -> new ArrayList<>())
                            .add(info);
                }
            }

            // Empfohlenes Mapping erstellen
            result.setRecommendedMapping(createRecommendedMapping(result.getAllFields()));
        }

        log.info("PDF analysiert: {} Felder gefunden", result.getAllFields().size());
        return result;
    }

    /**
     * Analysiert ein einzelnes Feld und extrahiert Metadaten.
     */
    private FieldInfo analyzeField(PDField field) {
        FieldInfo info = new FieldInfo();
        info.setFullName(field.getFullyQualifiedName());
        info.setType(field.getClass().getSimpleName());
        // Aufruf angepasst
        info.setCheckbox(field instanceof PDCheckBox);
        info.setCurrentValue(field.getValueAsString());

        String name = field.getFullyQualifiedName().toLowerCase();
        info.setShortName(extractShortName(name));

        // Abschnitt erkennen (z.B. "A.", "B.", "1.", "2." etc.)
        info.setSectionNumber(extractSectionNumber(name));

        // Person erkennen (Person 1, Person 2, etc.)
        info.setPersonNumber(extractPersonNumber(name));

        // Kategorie bestimmen
        info.setCategory(determineCategory(name));

        return info;
    }

    private String extractShortName(String fullName) {
        // Letzten Teil des Namens extrahieren (nach dem letzten Punkt oder Unterstrich)
        String[] parts = fullName.split("[._]");
        return parts[parts.length - 1];
    }

    private int extractSectionNumber(String name) {
        // Suche nach Abschnittsnummern
        if (name.contains("abschnitt")) {
            for (int i = 1; i <= 20; i++) {
                if (name.contains("abschnitt " + i) || name.contains("abschnitt" + i)) {
                    return i;
                }
            }
        }
        // Alternative: A=1, B=2, C=3, etc.
        if (name.startsWith("a.") || name.contains(".a.")) return 1;
        if (name.startsWith("b.") || name.contains(".b.")) return 2;
        if (name.startsWith("c.") || name.contains(".c.")) return 3;
        if (name.startsWith("d.") || name.contains(".d.")) return 4;
        if (name.startsWith("e.") || name.contains(".e.")) return 5;

        return 0;
    }

    private int extractPersonNumber(String name) {
        // Person 1, 2, 3, etc.
        for (int i = 1; i <= 10; i++) {
            if (name.contains("person " + i) || name.contains("person" + i) ||
                    name.contains("p" + i) || name.contains("_" + i + "_") ||
                    name.endsWith("_" + i) || name.endsWith("." + i)) {
                return i;
            }
        }
        // Antragsteller ist immer Person 1
        if (name.contains("antragsteller")) return 1;
        if (name.contains("ehegatte") || name.contains("partner")) return 2;

        return 0;
    }

    private String determineCategory(String name) {
        // Behörde
        if (containsAny(name, "behörde", "behoerde", "dienststelle", "amt", "wohngeldbehörde")) {
            return "BEHOERDE";
        }

        // Antragsdaten
        if (containsAny(name, "erstantrag", "weiterleistung", "folgeantrag", "aktenzeichen",
                "wohngeldnummer", "antragsdatum")) {
            return "ANTRAGSDATEN";
        }

        // Persönliche Daten
        if (containsAny(name, "name", "vorname", "nachname", "familienname", "geburt",
                "geschlecht", "männlich", "weiblich", "divers", "familienstand",
                "ledig", "verheiratet", "geschieden", "verwitwet")) {
            return "PERSON";
        }

        // Adresse
        if (containsAny(name, "straße", "strasse", "hausnummer", "hausnr", "plz",
                "postleitzahl", "ort", "stadt", "wohnort", "anschrift", "adresse")) {
            return "ADRESSE";
        }

        // Wohnung
        if (containsAny(name, "wohnung", "wohnfläche", "wohnflaeche", "zimmer", "räume",
                "einzug", "bezug", "baujahr", "vermieter", "mieter")) {
            return "WOHNUNG";
        }

        // Miete
        if (containsAny(name, "miete", "kaltmiete", "warmmiete", "nebenkosten",
                "heizkosten", "betriebskosten")) {
            return "MIETE";
        }

        // Einkommen
        if (containsAny(name, "einkommen", "verdienst", "gehalt", "lohn", "brutto",
                "netto", "rente", "kindergeld", "unterhalt")) {
            return "EINKOMMEN";
        }

        // Erwerbsstatus
        if (containsAny(name, "erwerbstätig", "erwerbstaetig", "arbeitslos", "rentner",
                "student", "schüler", "schueler", "selbständig", "selbststaendig")) {
            return "ERWERBSSTATUS";
        }

        // Bankverbindung
        if (containsAny(name, "iban", "bic", "bank", "konto", "kreditinstitut")) {
            return "BANK";
        }

        // Ja/Nein Felder
        if (containsAny(name, "ja", "nein", "yes", "no")) {
            return "JA_NEIN";
        }

        return "SONSTIGE";
    }

    /**
     * Erstellt ein empfohlenes Mapping von Feldnamen zu Datenwerten.
     */
    private Map<String, String> createRecommendedMapping(List<FieldInfo> fields) {
        Map<String, String> mapping = new LinkedHashMap<>();

        for (FieldInfo field : fields) {
            String fullName = field.getFullName();
            String name = fullName.toLowerCase();
            String value = determineValueForField(field);

            if (value != null) {
                mapping.put(fullName, value);
            }
        }

        return mapping;
    }

    /**
     * Bestimmt den passenden Dummy-Wert für ein Feld.
     */
    private String determineValueForField(FieldInfo field) {
        String name = field.getFullName().toLowerCase();
        int person = field.getPersonNumber();

        // Nur Person 1 (Antragsteller) ausfüllen
        if (person > 1) {
            return null;
        }

        // === BEHÖRDE ===
        if (field.getCategory().equals("BEHOERDE")) {
            if (containsAny(name, "name", "bezeichnung") && !containsAny(name, "straße", "strasse", "plz", "ort")) {
                return "Bezirksamt Mitte von Berlin";
            }
            if (containsAny(name, "straße", "strasse")) {
                return "Karl-Marx-Allee 31";
            }
            if (containsAny(name, "plz")) {
                return "10178";
            }
            if (containsAny(name, "ort")) {
                return "Berlin";
            }
        }

        // === ANTRAGSDATEN ===
        if (containsAny(name, "wohngeldnummer", "aktenzeichen") && !containsAny(name, "datum")) {
            return ""; // Leer bei Erstantrag
        }
        if (containsAny(name, "wohngeld ab", "ab datum", "leistung ab", "bewilligung ab")) {
            return "01.01.2026";
        }

        // === CHECKBOX: Erstantrag ===
        // Aufruf angepasst: Lombok-Getter ist jetzt `isCheckbox()`
        if (field.isCheckbox()) {
            if (containsAny(name, "erstantrag") && !containsAny(name, "weiter", "folge")) {
                return "true"; // Wird als Checkbox-Check interpretiert
            }
            if (containsAny(name, "weiterleistung", "folgeantrag")) {
                return "false";
            }
            // Geschlecht
            if (containsAny(name, "männlich", "maennlich") && !containsAny(name, "weiblich")) {
                return "true";
            }
            if (containsAny(name, "weiblich")) {
                return "false";
            }
            if (containsAny(name, "divers")) {
                return "false";
            }
            // Familienstand
            if (containsAny(name, "ledig") && !containsAny(name, "verheiratet", "geschieden")) {
                return "true";
            }
            // Andere Familienstand-Optionen
            if (containsAny(name, "verheiratet", "geschieden", "verwitwet", "getrennt", "lebenspartner")) {
                return "false";
            }
            // Erwerbsstatus
            if (containsAny(name, "erwerbstätig", "erwerbstaetig") && !containsAny(name, "nicht")) {
                return "true";
            }
            if (containsAny(name, "arbeitslos", "rentner", "student", "schüler", "schueler", "selbständig", "selbststaendig")) {
                return "false";
            }
            // Alle anderen Checkboxen: nicht setzen
            return null;
        }

        // === PERSON (Antragsteller) ===
        if (containsAny(name, "familienname", "nachname") && !containsAny(name, "vorname", "geburts")) {
            return "Mustermann";
        }
        if (containsAny(name, "vorname") && !containsAny(name, "nachname")) {
            return "Max";
        }
        if (containsAny(name, "geburtsdatum", "geb.datum", "geboren am")) {
            return "15.03.1985";
        }
        if (containsAny(name, "geburtsort", "geb.ort")) {
            return "Berlin";
        }
        if (containsAny(name, "staatsangehörigkeit", "staatsangehoerigkeit", "nationalität")) {
            return "deutsch";
        }
        if (containsAny(name, "telefon", "tel.")) {
            return "030-12345678";
        }
        if (containsAny(name, "email", "e-mail")) {
            return "max.mustermann@beispiel.de";
        }

        // === ADRESSE (nur wenn nicht Behörde) ===
        if (field.getCategory().equals("ADRESSE")) {
            if (containsAny(name, "straße", "strasse", "str.") && !containsAny(name, "hausnr", "nr")) {
                return "Musterstraße";
            }
            if (containsAny(name, "hausnummer", "hausnr", "haus-nr", "nr.")) {
                return "42";
            }
            if (containsAny(name, "postleitzahl", "plz")) {
                return "10115";
            }
            if (containsAny(name, "wohnort", "ort", "stadt") && !containsAny(name, "geburts")) {
                return "Berlin";
            }
        }

        // === WOHNUNG ===
        if (containsAny(name, "einzug", "bezug", "eingezogen", "seit wann")) {
            return "01.01.2023";
        }
        if (containsAny(name, "wohnfläche", "wohnflaeche", "qm", "größe")) {
            return "65";
        }
        if (containsAny(name, "zimmer", "räume", "raeume") && containsAny(name, "anzahl")) {
            return "3";
        }
        if (containsAny(name, "baujahr")) {
            return "1990";
        }
        if (containsAny(name, "vermieter") && containsAny(name, "name")) {
            return "Berliner Wohnungsbaugesellschaft mbH";
        }

        // === MIETE ===
        if (containsAny(name, "kaltmiete", "grundmiete", "miete ohne")) {
            return "650,00";
        }
        if (containsAny(name, "nebenkosten", "betriebskosten")) {
            return "150,00";
        }
        if (containsAny(name, "heizkosten", "heizung") && !containsAny(name, "art")) {
            return "80,00";
        }
        if (containsAny(name, "warmwasser")) {
            return "20,00";
        }
        if (containsAny(name, "gesamtmiete", "warmmiete", "miete gesamt", "insgesamt")) {
            return "900,00";
        }

        // === EINKOMMEN ===
        if (containsAny(name, "brutto") && containsAny(name, "einkommen", "verdienst", "gehalt")) {
            return "2.200,00";
        }
        if (containsAny(name, "netto") && containsAny(name, "einkommen", "verdienst", "gehalt")) {
            return "1.800,00";
        }

        // === BANKVERBINDUNG ===
        if (containsAny(name, "iban")) {
            return "DE89 3704 0044 0532 0130 00";
        }
        if (containsAny(name, "bic")) {
            return "COBADEFFXXX";
        }
        if (containsAny(name, "bank", "kreditinstitut", "geldinstitut")) {
            return "Commerzbank AG";
        }
        if (containsAny(name, "kontoinhaber")) {
            return "Max Mustermann";
        }

        // === HAUSHALT ===
        if (containsAny(name, "anzahl") && containsAny(name, "person", "haushalt")) {
            return "1";
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gibt eine formatierte Zusammenfassung der Analyse aus.
     */
    public String formatAnalysisReport(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PDF FELD-ANALYSE ===\n\n");

        sb.append("Gesamt: ").append(result.getAllFields().size()).append(" Felder\n\n");

        // Nach Kategorie
        sb.append("--- Nach Kategorie ---\n");
        for (Map.Entry<String, List<FieldInfo>> entry : result.getByCategory().entrySet()) {
            sb.append(String.format("%s: %d Felder\n", entry.getKey(), entry.getValue().size()));
            for (FieldInfo field : entry.getValue()) {
                sb.append(String.format("  - %s [%s]\n", field.getFullName(), field.getType()));
            }
        }

        sb.append("\n--- Empfohlenes Mapping ---\n");
        for (Map.Entry<String, String> entry : result.getRecommendedMapping().entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append(String.format("%s = %s\n", entry.getKey(), entry.getValue()));
            }
        }

        return sb.toString();
    }
}