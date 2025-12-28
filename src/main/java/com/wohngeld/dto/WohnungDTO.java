package com.wohngeld.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for housing/apartment details.
 *
 * Based on meeting transcript:
 * - MANDATORY: wohnflaecheQm, wohnverhaeltnis
 * - Most clients are HAUPTMIETER
 * - HEIMBEWOHNER is second most common case (stationäre Pflegeheime)
 * - verwandtschaftMitVermieter default NEIN
 * - mietpreisbindung default NEIN (JA only with Wohnberechtigungsschein)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WohnungDTO {

    /**
     * Living space in square meters (Fläche in Quadratmetern).
     * MANDATORY - from Mietvertrag (Question 22)
     */
    @NotNull(message = "Wohnfläche ist erforderlich")
    @Positive(message = "Wohnfläche muss positiv sein")
    private Double wohnflaecheQm;

    /**
     * Housing type (Question 21):
     * - HAUPTMIETER (most common)
     * - UNTERMIETER
     * - HEIMBEWOHNER (stationäre Wohnform - second most common)
     * - EIGENTUM
     * - SONSTIGE
     */
    @NotNull(message = "Wohnverhältnis ist erforderlich")
    @Builder.Default
    private String wohnverhaeltnis = "HAUPTMIETER";

    /**
     * Is there a family relationship with the landlord? (Question 21)
     * Default NEIN
     */
    @Builder.Default
    private boolean verwandtschaftMitVermieter = false;

    /**
     * Is the housing subsidized/Sozialwohnung? (Question 3)
     * Default NEIN
     * Set to JA only if person has Wohnberechtigungsschein (WBS)
     */
    @Builder.Default
    private boolean mietpreisbindung = false;

    /**
     * Has Wohnberechtigungsschein - implies mietpreisbindung = true
     */
    @Builder.Default
    private boolean wohnberechtigungsschein = false;

    // OPTIONAL fields (from Mietvertrag if needed)
    private String einzugsdatum;
    private Integer anzahlRaeume;
    private String baujahr;
    private String vermieterName;
}
