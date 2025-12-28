package com.wohngeld.mapper;

import com.wohngeld.dto.*;
import com.wohngeld.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between API DTOs and internal domain models.
 * The API DTOs are user-friendly, while internal models match the PDF structure.
 */
@Component
public class WohngeldAntragMapper {

    /**
     * Convert API request DTO to internal model.
     */
    public WohngeldAntragRequest toInternal(WohngeldAntragRequestDTO dto) {
        return WohngeldAntragRequest.builder()
                .antragsteller(mapAntragsteller(dto.getAntragsteller()))
                .adresse(mapAdresse(dto.getAdresse()))
                .wohnung(mapWohnung(dto.getWohnung()))
                .miete(mapMiete(dto.getMiete()))
                .einkommen(mapEinkommen(dto.getEinkommen()))
                .bankverbindung(mapBankverbindung(dto.getBankverbindung()))
                .antrag(mapAntragDaten(dto.getAntrag()))
                .haushalt(Haushalt.builder().anzahlPersonen(1).build())
                .build();
    }

    private Antragsteller mapAntragsteller(AntragstellerDTO dto) {
        return Antragsteller.builder()
                .nachname(dto.getFamilienname())
                .vorname(dto.getVorname())
                .geburtsdatum(dto.getGeburtsdatum())
                .geburtsort(dto.getGeburtsort())
                .geburtsname(dto.getGeburtsname())
                .staatsangehoerigkeit(dto.getStaatsangehoerigkeit())
                .geschlecht(normalizeGeschlecht(dto.getGeschlecht()))
                .familienstand(normalizeFamilienstand(dto.getFamilienstand()))
                .erwerbsstatus(normalizeErwerbsstatus(dto.getErwerbsstatus()))
                .telefon(dto.getTelefon())
                .email(dto.getEmail())
                .build();
    }

    private Adresse mapAdresse(AdresseDTO dto) {
        return Adresse.builder()
                .strasse(dto.getStrasse())
                .hausnummer(dto.getHausnummer())
                .plz(dto.getPlz())
                .ort(dto.getOrt())
                .bundesland(dto.getBundesland())
                .build();
    }

    private Wohnung mapWohnung(WohnungDTO dto) {
        return Wohnung.builder()
                .wohnflaecheQm(dto.getWohnflaecheQm())
                .einzugsdatum(dto.getEinzugsdatum())
                .anzahlRaeume(dto.getAnzahlRaeume())
                .baujahr(dto.getBaujahr())
                .vermieterName(dto.getVermieterName())
                .wohnverhaeltnis(normalizeWohnverhaeltnis(dto.getWohnverhaeltnis()))
                .verwandtschaftMitVermieter(dto.isVerwandtschaftMitVermieter())
                .mietpreisbindung(dto.isMietpreisbindung())
                .build();
    }

    private String normalizeWohnverhaeltnis(String value) {
        if (value == null) return "HAUPTMIETER";
        return switch (value.toUpperCase()) {
            case "HAUPTMIETER" -> "HAUPTMIETER";
            case "UNTERMIETER" -> "UNTERMIETER";
            case "HEIMBEWOHNER" -> "HEIMBEWOHNER";
            case "EIGENTUM", "EIGENTUEMER", "EIGENTÜMER" -> "EIGENTUM";
            default -> value.toUpperCase();
        };
    }

    private Miete mapMiete(MieteDTO dto) {
        return Miete.builder()
                .gesamtmiete(dto.getGesamtmiete())
                .heizkosten(dto.getHeizkosten())
                .warmwasser(dto.getWarmwasserkosten())
                .kaltmiete(calculateKaltmiete(dto))
                .nebenkosten(dto.getSonstigeKosten())
                .build();
    }

    private Double calculateKaltmiete(MieteDTO dto) {
        // Estimate Kaltmiete from Gesamtmiete minus known costs
        double kaltmiete = dto.getGesamtmiete();
        if (!dto.isHeizkostenEnthalten() && dto.getHeizkosten() != null) {
            kaltmiete -= dto.getHeizkosten();
        }
        if (!dto.isWarmwasserEnthalten() && dto.getWarmwasserkosten() != null) {
            kaltmiete -= dto.getWarmwasserkosten();
        }
        if (dto.getSonstigeKosten() != null) {
            kaltmiete -= dto.getSonstigeKosten();
        }
        return Math.max(0, kaltmiete);
    }

    private Einkommen mapEinkommen(EinkommenDTO dto) {
        // Calculate total from all income sources
        double totalBrutto = dto.getEinnahmen().stream()
                .mapToDouble(e -> convertToMonthly(e.getBruttoBetrag(), e.getTurnus()))
                .sum();

        // Map individual income entries
        List<Einnahme> einnahmen = dto.getEinnahmen().stream()
                .map(e -> Einnahme.builder()
                        .art(e.getArt())
                        .bruttoBetrag(e.getBruttoBetrag())
                        .turnus(e.getTurnus())
                        .build())
                .collect(Collectors.toList());

        return Einkommen.builder()
                .einnahmen(einnahmen)
                .bruttoeinkommenMonatlich(totalBrutto)
                .nettoeinkommenMonatlich(totalBrutto) // Simplified - same as brutto
                .steuern(dto.isSteuern())
                .rentenversicherung(dto.isRentenversicherung())
                .krankenPflegeversicherung(dto.isKrankenPflegeversicherung())
                .build();
    }

    private double convertToMonthly(Double amount, String turnus) {
        if (amount == null) return 0.0;
        return switch (turnus.toUpperCase()) {
            case "JAEHRLICH" -> amount / 12.0;
            case "TAEGLICH" -> amount * 30.0;
            default -> amount; // MONATLICH
        };
    }

    private Bankverbindung mapBankverbindung(BankverbindungDTO dto) {
        // Remove spaces from IBAN
        String cleanIban = dto.getIban().replaceAll("\\s", "");

        return Bankverbindung.builder()
                .iban(cleanIban)
                .bank(dto.getBankName())
                .bic(dto.getBic())
                .kontoinhaber(dto.getKontoinhaberFamilienname() + ", " + dto.getKontoinhaberVorname())
                .build();
    }

    private AntragDaten mapAntragDaten(AntragMetadatenDTO dto) {
        return AntragDaten.builder()
                .erstantrag(dto.getErstantrag())
                .wohngeldnummer(dto.getWohngeldnummer())
                .wohngeldAb(dto.getWohngeldAb())
                .antragsdatum(dto.getAntragsdatum())
                .build();
    }

    // Normalization methods for enum-like string values

    private String normalizeGeschlecht(String value) {
        if (value == null) return "maennlich";
        return switch (value.toUpperCase()) {
            case "MAENNLICH", "M", "MÄNNLICH" -> "maennlich";
            case "WEIBLICH", "W" -> "weiblich";
            case "DIVERS", "D" -> "divers";
            default -> value.toLowerCase();
        };
    }

    private String normalizeFamilienstand(String value) {
        if (value == null) return "ledig";
        return switch (value.toUpperCase()) {
            case "LEDIG" -> "ledig";
            case "VERHEIRATET" -> "verheiratet";
            case "GESCHIEDEN" -> "geschieden";
            case "VERWITWET" -> "verwitwet";
            case "GETRENNT_LEBEND", "GETRENNT LEBEND" -> "getrennt lebend";
            case "LEBENSPARTNERSCHAFT", "EINGETRAGENE_LEBENSPARTNERSCHAFT" -> "eingetragene Lebenspartnerschaft";
            default -> value.toLowerCase();
        };
    }

    private String normalizeErwerbsstatus(String value) {
        if (value == null) return "erwerbstaetig";
        return switch (value.toUpperCase()) {
            case "ERWERBSTAETIG", "ERWERBSTÄTIG" -> "erwerbstaetig";
            case "ARBEITSLOS" -> "arbeitslos";
            case "RENTNER", "RENTNERIN", "PENSIONAER", "PENSIONÄR" -> "rentner";
            case "STUDENT", "STUDENTIN" -> "student";
            case "SCHUELER", "SCHÜLER", "SCHUELERIN", "SCHÜLERIN" -> "schueler";
            case "SELBSTSTAENDIG", "SELBSTÄNDIG", "SELBSTSTÄNDIG" -> "selbststaendig";
            default -> value.toLowerCase();
        };
    }
}
