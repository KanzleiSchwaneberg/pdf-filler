package com.wohngeld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for bank account details.
 * IBAN is accepted as a single string and will be split internally for PDF fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankverbindungDTO {

    /**
     * Full IBAN as a single string (e.g., "DE72100900003280110000").
     * Spaces are allowed and will be removed internally.
     * Will be split into individual characters for PDF fields (IBAN1-IBAN33).
     */
    @NotBlank(message = "IBAN ist erforderlich")
    @Pattern(regexp = "[A-Z]{2}[0-9A-Z\\s]{13,32}", message = "Ungültiges IBAN-Format")
    private String iban;

    @NotBlank(message = "Name der Bank ist erforderlich")
    private String bankName;

    /**
     * Account holder - will be used for Familienname field in bank section.
     */
    @NotBlank(message = "Kontoinhaber Familienname ist erforderlich")
    private String kontoinhaberFamilienname;

    /**
     * Account holder first name - will be used for Vorname field in bank section.
     */
    @NotBlank(message = "Kontoinhaber Vorname ist erforderlich")
    private String kontoinhaberVorname;

    /**
     * Full address for bank account holder (Straße, Hausnummer, PLZ, Ort).
     */
    @NotBlank(message = "Kontoinhaber Anschrift ist erforderlich")
    private String kontoinhaberAnschrift;

    private String bic;
}
