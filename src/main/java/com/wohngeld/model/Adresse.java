package com.wohngeld.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adresse {

    @NotBlank(message = "Straße ist erforderlich")
    private String strasse;

    @NotBlank(message = "Hausnummer ist erforderlich")
    private String hausnummer;

    @NotBlank(message = "PLZ ist erforderlich")
    private String plz;

    @NotBlank(message = "Ort ist erforderlich")
    private String ort;

    private String bundesland;

    public String getFullAddress() {
        return strasse + " " + hausnummer + ", " + plz + " " + ort;
    }
}
