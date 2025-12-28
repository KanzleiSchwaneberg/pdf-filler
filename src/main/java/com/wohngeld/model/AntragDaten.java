package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntragDaten {

    // Antragsdatum (Datum der Antragstellung)
    @Builder.Default
    private String antragsdatum = LocalDate.now().toString();

    // Wohngeld ab (Datum ab wann Wohngeld beantragt wird)
    private String wohngeldAb;

    // true = Erstantrag, false = Weiterleistungsantrag
    @Builder.Default
    private Boolean erstantrag = true;

    // Wohngeldnummer / Aktenzeichen (nur bei Weiterleistungsantrag)
    private String wohngeldnummer;

    // Anschrift der Wohngeldbehörde
    private String wohngeldbehoerde;
    private String wohngeldbehoerdeStrasse;
    private String wohngeldbehoerdePlz;
    private String wohngeldbehoerdeOrt;
}
