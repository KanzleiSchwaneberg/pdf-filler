package com.wohngeld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for a single income source.
 * The PDF form supports multiple income entries per person.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EinnahmeDTO {

    /**
     * Type of income, e.g.:
     * - Erwerbsunfähigkeitsrente
     * - Gehalt/Lohn
     * - Arbeitslosengeld
     * - Rente
     * - Kindergeld
     * - etc.
     */
    @NotBlank(message = "Art der Einnahme ist erforderlich")
    private String art;

    @NotNull(message = "Bruttobetrag ist erforderlich")
    @PositiveOrZero(message = "Betrag muss positiv sein")
    private Double bruttoBetrag;

    /**
     * Frequency: MONATLICH, JAEHRLICH, TAEGLICH
     */
    @NotBlank(message = "Turnus ist erforderlich")
    @Builder.Default
    private String turnus = "MONATLICH";
}
