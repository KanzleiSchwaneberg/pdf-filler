package com.wohngeld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wohngeld.dto.*;
import com.wohngeld.mapper.WohngeldAntragMapper;
import com.wohngeld.model.*;
import com.wohngeld.service.PdfFieldAnalyzer;
import com.wohngeld.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wohngeld API", description = "API für Wohngeldantrag-Automatisierung")
public class WohngeldController {

    private final PdfService pdfService;
    private final PdfFieldAnalyzer pdfFieldAnalyzer;
    private final WohngeldAntragMapper antragMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    @Operation(summary = "API Info", description = "Gibt Informationen über die API zurück")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Wohngeld-Automation API");
        info.put("version", "1.0.0");
        info.put("endpoints", Map.of(
                "/api/fill-pdf", "POST - PDF mit Antragsdaten ausfüllen (legacy)",
                "/api/v2/fill-pdf", "POST - PDF mit neuen DTO-Antragsdaten ausfüllen",
                "/api/fields", "GET - Liste aller PDF-Feldnamen",
                "/api/process", "POST - Kompletter Prozess (Ausfüllen + optional Email)",
                "/api/health", "GET - Health Check",
                "/api/data/sample", "GET - Beispiel-Datenstruktur (legacy)",
                "/api/v2/data/sample", "GET - Beispiel-Datenstruktur (neue DTOs)"
        ));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Prüft ob der Service läuft")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", LocalDateTime.now().toString());
        //health.put("emailServiceEnabled", emailService.map(EmailService::isEnabled).orElse(false));
        return ResponseEntity.ok(health);
    }

    @GetMapping("/data/sample")
    @Operation(summary = "Beispieldaten", description = "Gibt Beispiel-Antragsdaten zurück")
    public ResponseEntity<WohngeldAntragRequest> getSampleData() {
        WohngeldAntragRequest sample = createSampleData();
        return ResponseEntity.ok(sample);
    }

    @GetMapping("/fields")
    @Operation(summary = "Formularfelder", description = "Listet alle Formularfelder der PDF auf")
    public ResponseEntity<ApiResponse<List<String>>> getFormFields(
            @RequestParam(required = false) String templatePath
    ) {
        try {
            List<String> fields = pdfService.getFormFields(templatePath);
            return ResponseEntity.ok(ApiResponse.success(
                    "Gefundene Felder: " + fields.size(),
                    fields
            ));
        } catch (IOException e) {
            log.error("Fehler beim Lesen der Formularfelder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fehler: " + e.getMessage()));
        }
    }

    @GetMapping("/analyze")
    @Operation(summary = "PDF-Felder analysieren", description = "Analysiert die PDF-Felder und zeigt Kategorisierung und empfohlenes Mapping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzePdf(
            @RequestParam(required = false) String templatePath
    ) {
        try {
            String path = templatePath != null ? templatePath : pdfService.getDefaultTemplatePath();
            PdfFieldAnalyzer.AnalysisResult analysis = pdfFieldAnalyzer.analyzePdf(path);

            Map<String, Object> result = new HashMap<>();
            result.put("totalFields", analysis.getAllFields().size());
            result.put("fieldsByCategory", analysis.getByCategory().entrySet().stream()
                    .collect(HashMap::new,
                            (m, e) -> m.put(e.getKey(), e.getValue().stream()
                                    .map(f -> Map.of(
                                            "name", f.getFullName(),
                                            "type", f.getType(),
                                            "isCheckbox", f.isCheckbox(),
                                            "personNumber", f.getPersonNumber()
                                    ))
                                    .toList()),
                            HashMap::putAll));
            result.put("recommendedMapping", analysis.getRecommendedMapping());
            result.put("report", pdfFieldAnalyzer.formatAnalysisReport(analysis));

            return ResponseEntity.ok(ApiResponse.success(
                    "PDF analysiert: " + analysis.getAllFields().size() + " Felder gefunden",
                    result
            ));
        } catch (IOException e) {
            log.error("Fehler bei der PDF-Analyse: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fehler: " + e.getMessage()));
        }
    }

    @PostMapping("/fill-pdf")
    @Operation(summary = "PDF ausfüllen", description = "Füllt die PDF-Vorlage mit den Antragsdaten aus")
    public ResponseEntity<ApiResponse<PdfResult>> fillPdf(
            @Valid @RequestBody WohngeldAntragRequest request,
            @RequestParam(required = false) String templatePath
    ) {
        try {
            PdfResult result = pdfService.fillPdf(request, templatePath);
            return ResponseEntity.ok(ApiResponse.success("PDF erfolgreich erstellt", result));
        } catch (IOException e) {
            log.error("Fehler beim PDF-Ausfüllen: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fehler: " + e.getMessage()));
        }
    }

//    @PostMapping("/send-email")
//    @Operation(summary = "Email senden", description = "Versendet den Antrag per Email")
//    public ResponseEntity<ApiResponse<String>> sendEmail(
//            @Valid @RequestBody EmailRequest request
//    ) {
//        if (emailService.isEmpty()) {
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error("Email-Funktion ist deaktiviert oder nicht konfiguriert"));
//        }
//
//        File pdfFile = new File(request.getPdfPath());
//        if (!pdfFile.exists()) {
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error("PDF nicht gefunden: " + request.getPdfPath()));
//        }
//
//        emailService.get().sendAntragEmail(
//                request.getRecipientEmail(),
//                request.getPdfPath(),
//                request.getAntragData()
//        );

//        return ResponseEntity.ok(ApiResponse.success(
//                "Email wird gesendet an: " + request.getRecipientEmail(),
//                request.getRecipientEmail()
//        ));
//    }

    @PostMapping("/process")
    @Operation(summary = "Kompletter Prozess", description = "Füllt PDF aus und sendet optional per Email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fullProcess(
            @Valid @RequestBody FullProcessRequest request
    ) {
        try {
            // PDF ausfüllen
            PdfResult pdfResult = pdfService.fillPdf(request.getAntragData(), null);

            Map<String, Object> result = new HashMap<>();
            result.put("pdfFilled", true);
            result.put("pdfPath", pdfResult.getOutputPath());
            result.put("pdfFilename", pdfResult.getFilename());
            result.put("fieldsFound", pdfResult.getFieldsFound());
            result.put("fieldsFilled", pdfResult.getFieldsFilled());

            // Email senden falls gewünscht und verfügbar
//            if (request.isSendEmail() && request.getRecipientEmail() != null && emailService.isPresent()) {
//                emailService.get().sendAntragEmail(
//                        request.getRecipientEmail(),
//                        pdfResult.getOutputPath(),
//                        request.getAntragData()
//                );
//                result.put("emailSent", true);
//                result.put("emailRecipient", request.getRecipientEmail());
//            } else {
//                result.put("emailSent", false);
//            }

            return ResponseEntity.ok(ApiResponse.success("Verarbeitung abgeschlossen", result));

        } catch (IOException e) {
            log.error("Fehler bei der Verarbeitung: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fehler: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    @Operation(summary = "PDF herunterladen", description = "Lädt eine erstellte PDF herunter")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String filename) {
        try {
            Path filePath = Path.of("output", filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Fehler beim Download: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== V2 API Endpoints (using DTOs) ====================

    @PostMapping("/v2/fill-pdf")
    @Operation(summary = "PDF ausfüllen (v2)", description = "Füllt die PDF-Vorlage mit den neuen DTO-Antragsdaten aus")
    public ResponseEntity<ApiResponse<PdfResult>> fillPdfV2(
            @Valid @RequestBody WohngeldAntragRequestDTO requestDto,
            @RequestParam(required = false) String templatePath
    ) {
        try {
            // Convert DTO to internal model
            WohngeldAntragRequest request = antragMapper.toInternal(requestDto);
            PdfResult result = pdfService.fillPdf(request, templatePath);
            return ResponseEntity.ok(ApiResponse.success("PDF erfolgreich erstellt", result));
        } catch (IOException e) {
            log.error("Fehler beim PDF-Ausfüllen (v2): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fehler: " + e.getMessage()));
        }
    }

    @GetMapping("/v2/data/sample")
    @Operation(summary = "Beispieldaten (v2)", description = "Gibt Beispiel-Antragsdaten im neuen DTO-Format zurück")
    public ResponseEntity<WohngeldAntragRequestDTO> getSampleDataV2() {
        WohngeldAntragRequestDTO sample = createSampleDataDTO();
        return ResponseEntity.ok(sample);
    }

    /**
     * Creates realistic sample data based on transcript analysis.
     * Typical case: Rentner with EU-Rente and Pflegegrad.
     */
    private WohngeldAntragRequestDTO createSampleDataDTO() {
        return WohngeldAntragRequestDTO.builder()
                .antrag(AntragMetadatenDTO.builder()
                        .erstantrag(true)
                        // wohngeldAb NOT needed for Erstantrag - counts from submission
                        .antragsdatum("21.12.2025")
                        .formloserAntragDatum("07.01.2025")  // If informal application was sent earlier
                        .build())
                .antragsteller(AntragstellerDTO.builder()
                        .familienname("Beispiel")
                        .vorname("Maria")
                        .geburtsdatum("23.05.1962")
                        .staatsangehoerigkeit("deutsch")
                        .geschlecht("WEIBLICH")
                        .familienstand("GESCHIEDEN")
                        .erwerbsstatus("RENTNER")  // Most common for this clientele
                        // geburtsname, geburtsort not needed
                        // telefon, email are optional ("freiwillig")
                        .build())
                .adresse(AdresseDTO.builder()
                        .strasse("Heilig-Geist-Str.")
                        .hausnummer("3")
                        .plz("14467")
                        .ort("Potsdam")
                        .bundesland("Brandenburg")
                        .build())
                .wohnung(WohnungDTO.builder()
                        .wohnflaecheQm(45.0)
                        .wohnverhaeltnis("HAUPTMIETER")  // Most common
                        .verwandtschaftMitVermieter(false)
                        .mietpreisbindung(false)  // Only true with WBS
                        .build())
                .miete(MieteDTO.builder()
                        .gesamtmiete(463.25)
                        .heizkostenEnthalten(false)
                        .heizkosten(61.50)
                        .warmwasserEnthalten(false)
                        .warmwasserkosten(0.0)
                        .sonstigeKosten(0.0)
                        .mietaenderung("NEIN")  // Always NEIN - send update if it changes
                        .build())
                .einkommen(EinkommenDTO.builder()
                        .einnahmen(List.of(
                                EinnahmeDTO.builder()
                                        .art("Erwerbsminderungsrente")  // Most common: EU-Rente
                                        .bruttoBetrag(855.42)
                                        .turnus("MONATLICH")
                                        .build(),
                                EinnahmeDTO.builder()
                                        .art("Zuschlag zur Rente")
                                        .bruttoBetrag(38.49)
                                        .turnus("MONATLICH")
                                        .build()
                        ))
                        .krankenPflegeversicherung(true)  // Usually JA - from Rentenbescheid
                        .rentenversicherung(false)  // Rentner don't pay this
                        .steuern(false)  // Only ~2% pay taxes
                        .build())
                .bankverbindung(BankverbindungDTO.builder()
                        .iban("DE89 3704 0044 0532 0130 00")
                        .bankName("Sparkasse Potsdam")
                        .kontoinhaberFamilienname("Beispiel")
                        .kontoinhaberVorname("Maria")
                        .kontoinhaberAnschrift("Heilig-Geist-Str. 3, 14467 Potsdam")
                        .build())
                .zusatzfragen(ZusatzfragenDTO.builder()
                        .andereWohnungWohngeld(false)
                        .zweitwohnsitz(false)
                        .transferleistungen(true)  // Grundsicherung beantragt
                        .transferleistungArt("GRUNDSICHERUNG")  // Type 2
                        .transferleistungDatum("07.01.2025")  // Date of formloser Antrag
                        .aufgefordertZuBeantragen(false)
                        .schwerbehinderungOderPflege(true)  // Common for this clientele
                        .pflegegrad("PG 2")
                        .haeuslichPflegebeduerftig(false)
                        .vermoegen(false)  // Under 60,000 EUR threshold
                        .einverstaendnisKontoauszuege(true)  // Usually JA
                        .build())
                .build();
    }

    private WohngeldAntragRequest createSampleData() {
        return WohngeldAntragRequest.builder()
                .antragsteller(Antragsteller.builder()
                        .anrede("Herr")
                        .geschlecht("maennlich")  // maennlich, weiblich, divers
                        .vorname("Max")
                        .nachname("Mustermann")
                        .geburtsdatum("15.03.1985")
                        .geburtsort("Berlin")
                        .staatsangehoerigkeit("deutsch")
                        .familienstand("ledig")  // ledig, verheiratet, geschieden, verwitwet, getrennt lebend
                        .erwerbsstatus("erwerbstaetig")  // erwerbstaetig, arbeitslos, rentner, student, schueler, selbststaendig
                        .telefon("030-12345678")
                        .email("max.mustermann@example.com")
                        .build())
                .adresse(Adresse.builder()
                        .strasse("Musterstraße")  // NUR die Straße, ohne Hausnummer!
                        .hausnummer("42")         // NUR die Hausnummer!
                        .plz("10115")             // NUR die PLZ!
                        .ort("Berlin")            // NUR der Ort!
                        .bundesland("Berlin")
                        .build())
                .wohnung(Wohnung.builder()
                        .einzugsdatum("01.01.2023")
                        .wohnflaecheQm(65.0)
                        .anzahlRaeume(3)
                        .heizungsart("Zentralheizung")
                        .baujahr("1990")
                        .vermieterName("Immobilien GmbH")
                        .vermieterAdresse("Vermieterweg 1, 10117 Berlin")
                        .build())
                .miete(Miete.builder()
                        .kaltmiete(650.0)
                        .nebenkosten(150.0)
                        .heizkosten(80.0)
                        .warmwasser(20.0)
                        .build())
                .haushalt(Haushalt.builder()
                        .anzahlPersonen(1)
                        // Haushaltsmitglieder werden NICHT automatisch ausgefüllt
                        .build())
                .einkommen(Einkommen.builder()
                        .bruttoeinkommenMonatlich(2200.0)
                        .nettoeinkommenMonatlich(1800.0)
                        .sonstigeEinnahmen(0.0)
                        .kindergeld(0.0)
                        .unterhalt(0.0)
                        .build())
                .bankverbindung(Bankverbindung.builder()
                        .kontoinhaber("Max Mustermann")
                        .iban("DE89370400440532013000")
                        .bic("COBADEFFXXX")
                        .bank("Commerzbank")
                        .build())
                .antrag(AntragDaten.builder()
                        .erstantrag(true)  // true = Erstantrag, false = Weiterleistungsantrag
                        .wohngeldAb("01.01.2026")
                        .wohngeldnummer("")  // Nur bei Weiterleistungsantrag
                        .wohngeldbehoerde("Bezirksamt Mitte")
                        .wohngeldbehoerdeStrasse("Rathausstraße 1")
                        .wohngeldbehoerdePlz("10178")
                        .wohngeldbehoerdeOrt("Berlin")
                        .build())
                .build();
    }
}
