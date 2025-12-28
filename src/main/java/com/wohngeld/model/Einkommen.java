package com.wohngeld.model;

import jakarta.validation.constraints.PositiveOrZero;
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
public class Einkommen {

    /**
     * List of income sources (new structured format).
     */
    @Builder.Default
    private List<Einnahme> einnahmen = new ArrayList<>();

    /**
     * Legacy field - total gross monthly income.
     */
    @PositiveOrZero
    private Double bruttoeinkommenMonatlich;

    /**
     * Legacy field - total net monthly income.
     */
    @PositiveOrZero
    private Double nettoeinkommenMonatlich;

    @Builder.Default
    @PositiveOrZero
    private Double sonstigeEinnahmen = 0.0;

    @Builder.Default
    @PositiveOrZero
    private Double kindergeld = 0.0;

    @Builder.Default
    @PositiveOrZero
    private Double unterhalt = 0.0;

    /**
     * Pays taxes?
     */
    @Builder.Default
    private Boolean steuern = false;

    /**
     * Pays pension insurance?
     */
    @Builder.Default
    private Boolean rentenversicherung = false;

    /**
     * Pays health/care insurance?
     */
    @Builder.Default
    private Boolean krankenPflegeversicherung = true;
}
