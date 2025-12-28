package com.wohngeld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for address data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdresseDTO {

    @NotBlank(message = "Straße ist erforderlich")
    private String strasse;

    @NotBlank(message = "Hausnummer ist erforderlich")
    private String hausnummer;

    @NotBlank(message = "PLZ ist erforderlich")
    @Pattern(regexp = "\\d{5}", message = "PLZ muss 5 Ziffern haben")
    private String plz;

    @NotBlank(message = "Ort ist erforderlich")
    private String ort;

    private String bundesland;
}
