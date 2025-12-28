package com.wohngeld.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for applicant (Antragsteller) data.
 *
 * Based on meeting transcript analysis:
 * - MANDATORY: familienname, vorname, geburtsdatum, geschlecht, familienstand, erwerbsstatus
 * - OPTIONAL: geburtsname, geburtsort, telefon, email (not needed for processing)
 * - staatsangehoerigkeit defaults to "deutsch"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntragstellerDTO {

    @NotBlank(message = "Familienname ist erforderlich")
    private String familienname;

    @NotBlank(message = "Vorname ist erforderlich")
    private String vorname;

    @NotBlank(message = "Geburtsdatum ist erforderlich (Format: TT.MM.JJJJ)")
    private String geburtsdatum;

    @Builder.Default
    private String staatsangehoerigkeit = "deutsch";

    /**
     * Gender: MAENNLICH, WEIBLICH, DIVERS
     */
    @NotBlank(message = "Geschlecht ist erforderlich")
    private String geschlecht;

    /**
     * Marital status: LEDIG, VERHEIRATET, GESCHIEDEN, VERWITWET, GETRENNT_LEBEND, LEBENSPARTNERSCHAFT
     */
    @NotBlank(message = "Familienstand ist erforderlich")
    private String familienstand;

    /**
     * Employment status: ERWERBSTAETIG, ARBEITSLOS, RENTNER, STUDENT, SCHUELER, SELBSTSTAENDIG, SONSTIGE
     * Most common for this clientele: RENTNER
     */
    @NotBlank(message = "Erwerbsstatus ist erforderlich")
    private String erwerbsstatus;

    // OPTIONAL fields (not needed for processing according to transcript)
    private String geburtsname;  // Rarely needed
    private String geburtsort;   // Rarely needed
    private String telefon;      // "freiwillig" - optional
    private String email;        // "freiwillig" - optional
}
