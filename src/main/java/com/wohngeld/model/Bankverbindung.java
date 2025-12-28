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
public class Bankverbindung {

    @NotBlank(message = "Kontoinhaber ist erforderlich")
    private String kontoinhaber;

    @NotBlank(message = "IBAN ist erforderlich")
    private String iban;

    private String bic;
    private String bank;
}
