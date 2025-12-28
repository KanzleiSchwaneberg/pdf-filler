package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Haushaltsmitglied {

    private String vorname;
    private String nachname;
    private String geburtsdatum;
    private String verwandtschaft;

    @Builder.Default
    private Double einkommenMonatlich = 0.0;
}
