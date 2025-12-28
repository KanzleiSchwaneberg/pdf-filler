package com.wohngeld.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Miete {

    @NotNull(message = "Kaltmiete ist erforderlich")
    @PositiveOrZero(message = "Kaltmiete muss positiv sein")
    private Double kaltmiete;

    @Builder.Default
    @PositiveOrZero
    private Double nebenkosten = 0.0;

    @Builder.Default
    @PositiveOrZero
    private Double heizkosten = 0.0;

    @Builder.Default
    @PositiveOrZero
    private Double warmwasser = 0.0;

    private Double gesamtmiete;

    public Double getGesamtmiete() {
        if (gesamtmiete != null) {
            return gesamtmiete;
        }
        return kaltmiete + nebenkosten + heizkosten + warmwasser;
    }
}
