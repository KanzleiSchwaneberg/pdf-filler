package com.wohngeld.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for application metadata.
 *
 * Based on meeting transcript:
 * - wohngeldAb is NOT needed for Erstantrag (counts from submission date)
 * - For Erstantrag, often a "formloser Antrag" is sent first via email
 * - wohngeldnummer is MANDATORY for Weiterleistungsantrag (helps Behörde find records)
 * - Anschrift der Wohngeldbehörde is NOT needed (sent via email/EGVP)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntragMetadatenDTO {

    /**
     * Application type: true = Erstantrag, false = Weiterleistungsantrag/Erhöhungsantrag
     * Most cases are Erstantrag
     */
    @NotNull(message = "Antragstyp ist erforderlich")
    @Builder.Default
    private Boolean erstantrag = true;

    /**
     * Existing Wohngeld number (Aktenzeichen).
     * MANDATORY for Weiterleistungsantrag - helps Behörde find existing records.
     * NOT needed for Erstantrag.
     */
    private String wohngeldnummer;

    /**
     * Date from which Wohngeld is requested (Format: TT.MM.JJJJ).
     * NOT NEEDED for Erstantrag - automatically counts from submission.
     * Only relevant for Erhöhungsantrag (e.g., after moving to more expensive apartment).
     */
    private String wohngeldAb;

    /**
     * Application date (Format: TT.MM.JJJJ).
     * If not provided, current date will be used.
     */
    private String antragsdatum;

    /**
     * Date of informal application (formloser Antrag) if sent earlier.
     * This date takes precedence for Wohngeld start date.
     */
    private String formloserAntragDatum;

    /**
     * Confirmation that applicant agrees to terms (question 31).
     * Always JA - required for submission
     */
    @Builder.Default
    private boolean einverstaendniserklaerung = true;
}
