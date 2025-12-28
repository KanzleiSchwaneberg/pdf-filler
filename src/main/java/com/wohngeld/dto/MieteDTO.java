package com.wohngeld.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for rent/cost details (Questions 23-27).
 *
 * Based on meeting transcript:
 * - MANDATORY: gesamtmiete
 * - Heizkosten and Warmwasserkosten are the relevant fields
 * - Tiefgarage/Stellplatz/Servicepauschale rarely apply
 * - Mietänderung: Always NEIN - if rent changes, send update later
 * - Data comes from Mietvertrag (must be attached)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MieteDTO {

    /**
     * Total rent including utilities (Gesamtmiete inkl. aller Nebenkosten).
     * MANDATORY - Question 23
     */
    @NotNull(message = "Gesamtmiete ist erforderlich")
    @PositiveOrZero(message = "Gesamtmiete muss positiv sein")
    private Double gesamtmiete;

    /**
     * Are heating costs included in rent? (Question 24)
     * If JA, specify amount in heizkosten
     */
    @Builder.Default
    private boolean heizkostenEnthalten = false;

    /**
     * Heating costs amount (if separate - Question 24).
     * This is commonly filled in.
     */
    @Builder.Default
    @PositiveOrZero
    private Double heizkosten = 0.0;

    /**
     * Are hot water costs included? (Question 24)
     * Less common than heating costs
     */
    @Builder.Default
    private boolean warmwasserEnthalten = false;

    /**
     * Hot water costs amount (if separate).
     */
    @Builder.Default
    @PositiveOrZero
    private Double warmwasserkosten = 0.0;

    /**
     * Other costs paid to third parties (e.g., garbage, cable) - Question 25.
     * Sometimes applies (e.g., Müllentsorgung, Kabelanschluss)
     */
    @Builder.Default
    @PositiveOrZero
    private Double sonstigeKosten = 0.0;

    /**
     * Will rent change in the next 12 months? (Question 27)
     * Always NEIN - if it changes, submit update later
     */
    @Builder.Default
    private String mietaenderung = "NEIN";
}
