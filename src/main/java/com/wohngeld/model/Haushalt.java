package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Haushalt {

    @Builder.Default
    private Integer anzahlPersonen = 1;

    @Builder.Default
    private List<Haushaltsmitglied> haushaltsmitglieder = new ArrayList<>();
}
