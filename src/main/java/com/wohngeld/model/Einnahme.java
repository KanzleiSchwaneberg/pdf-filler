package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single income source entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Einnahme {
    private String art;           // Type: "Erwerbsminderungsrente", "Altersrente", "Gehalt", etc.
    private Double bruttoBetrag;  // Gross amount
    private String turnus;        // Frequency: "monatlich", "jährlich"
}
