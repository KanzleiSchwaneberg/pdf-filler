package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wohnung {

    private String einzugsdatum;
    private Double wohnflaecheQm;
    private Integer anzahlRaeume;
    private String heizungsart;
    private String baujahr;
    private String vermieterName;
    private String vermieterAdresse;

    /**
     * Housing type: HAUPTMIETER, UNTERMIETER, HEIMBEWOHNER, EIGENTUM
     */
    @Builder.Default
    private String wohnverhaeltnis = "HAUPTMIETER";

    /**
     * Is there a family relationship with landlord?
     */
    @Builder.Default
    private Boolean verwandtschaftMitVermieter = false;

    /**
     * Is the housing subsidized (Sozialwohnung)?
     */
    @Builder.Default
    private Boolean mietpreisbindung = false;
}
