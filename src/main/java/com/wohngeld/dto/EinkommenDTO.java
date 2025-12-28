package com.wohngeld.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * API DTO for income data (Question 12).
 *
 * Based on meeting transcript:
 * - Most common income types for this clientele:
 *   1. Rente (EU-Rente / Erwerbsminderungsrente) - MOST COMMON
 *   2. Normale Altersrente
 *   3. Very rarely: Gehalt/Lohn, Krankengeld
 * - Usually only ONE income source
 * - Deductions: Most pay Kranken-/Pflegeversicherung, very few pay Steuern
 * - MANDATORY: At least one income source must be provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EinkommenDTO {

    /**
     * List of income sources (Art der Einnahme).
     * The PDF supports up to 4 income entries per person.
     *
     * Common types:
     * - "Erwerbsminderungsrente" / "EU-Rente"
     * - "Altersrente"
     * - "Gehalt/Lohn" (rare)
     * - "Krankengeld" (very rare)
     */
    @NotEmpty(message = "Mindestens eine Einnahmequelle ist erforderlich")
    @Valid
    @Builder.Default
    private List<EinnahmeDTO> einnahmen = new ArrayList<>();

    /**
     * Pays taxes? (Usually NEIN for this clientele - only ~2% pay taxes)
     */
    @Builder.Default
    private boolean steuern = false;

    /**
     * Pays pension insurance contributions?
     * Usually NEIN for Rentner (they receive, not pay)
     */
    @Builder.Default
    private boolean rentenversicherung = false;

    /**
     * Pays health/care insurance contributions?
     * Usually JA - this is common and noted in Rentenbescheid
     */
    @Builder.Default
    private boolean krankenPflegeversicherung = true;
}
