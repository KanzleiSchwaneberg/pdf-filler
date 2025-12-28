package com.wohngeld.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WohngeldAntragRequest {

    @NotNull(message = "Antragsteller ist erforderlich")
    @Valid
    private Antragsteller antragsteller;

    @NotNull(message = "Adresse ist erforderlich")
    @Valid
    private Adresse adresse;

    private Wohnung wohnung;

    @NotNull(message = "Miete ist erforderlich")
    @Valid
    private Miete miete;

    private Haushalt haushalt;

    @NotNull(message = "Einkommen ist erforderlich")
    @Valid
    private Einkommen einkommen;

    @NotNull(message = "Bankverbindung ist erforderlich")
    @Valid
    private Bankverbindung bankverbindung;

    private AntragDaten antrag;
}
