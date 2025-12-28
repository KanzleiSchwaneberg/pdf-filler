package com.wohngeld.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API DTO for additional yes/no questions on the form.
 *
 * Based on meeting transcript:
 * - Most questions default to "Nein" (false)
 * - Only set to true if the specific condition applies
 * - Most clients live alone, so Haushaltsmitglieder fields rarely apply
 *
 * RELEVANT transfer payments (mutually exclusive):
 * 1. Bürgergeld (SGB II)
 * 2. Grundsicherung (SGB XII)
 * 6. Ergänzende Hilfe zum Lebensunterhalt (BVG)
 * 7. Leistungen nach Asylbewerberleistungsgesetz
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZusatzfragenDTO {

    // NOTE: mietpreisbindung and wohnberechtigungsschein are in WohnungDTO (housing-related)

    /**
     * Question 4: Already receiving Wohngeld for another apartment?
     * Very rare case (Zweitwohnsitz) - default NEIN
     */
    @Builder.Default
    private boolean andereWohnungWohngeld = false;

    /**
     * Question 5: Do you have a secondary residence?
     * Very rare case - default NEIN
     */
    @Builder.Default
    private boolean zweitwohnsitz = false;

    /**
     * Question 8: Household member deceased in last 12 months?
     * Only relevant if person has Haushaltsmitglieder - default NEIN
     */
    @Builder.Default
    private boolean haushaltsmitgliedVerstorben = false;

    /**
     * Question 9: Will household size change in next 12 months?
     * Default NEIN
     */
    @Builder.Default
    private boolean haushaltsmitgliederAenderung = false;

    /**
     * Question 10: Receiving transfer payments (Transferleistungen)?
     * IMPORTANT: These are mutually exclusive with Wohngeld!
     * If receiving Bürgergeld/Grundsicherung, they already include housing costs.
     */
    @Builder.Default
    private boolean transferleistungen = false;

    /**
     * If transferleistungen = true, specify which type:
     * - BUERGERGELD (1)
     * - GRUNDSICHERUNG (2)
     * - ERGAENZENDE_HILFE (6)
     * - ASYLBEWERBERLEISTUNG (7)
     */
    private String transferleistungArt;

    /**
     * If transferleistungen = true, date of application (formlos).
     */
    private String transferleistungDatum;

    /**
     * Question 11: Asked by Jobcenter/Sozialamt to apply for Wohngeld?
     * Default NEIN - rarely happens
     */
    @Builder.Default
    private boolean aufgefordertZuBeantragen = false;

    /**
     * Question 13: Werbungskosten (advertising costs for work)?
     * Default NEIN - rare in this clientele
     */
    @Builder.Default
    private boolean werbungskosten = false;

    /**
     * Question 14: Kinderbetreuungskosten (childcare costs)?
     * Default NEIN - rare (most clients are older without children)
     */
    @Builder.Default
    private boolean kinderbetreuungskosten = false;

    /**
     * Question 15: Disability/care level (Schwerbehinderung/Pflegegrad)?
     * OFTEN TRUE for this clientele - check Butler database
     */
    @Builder.Default
    private boolean schwerbehinderungOderPflege = false;

    /**
     * If schwerbehinderungOderPflege = true, specify care level (e.g., "PG 2").
     */
    private String pflegegrad;

    /**
     * Schwerbehinderung degree if applicable (e.g., "50", "80")
     */
    private String schwerbehinderungGrad;

    /**
     * If schwerbehinderungOderPflege = true, check if receiving home care.
     */
    @Builder.Default
    private boolean haeuslichPflegebeduerftig = false;

    /**
     * Question 17: Unterhalt claim that couldn't be enforced?
     * Default NEIN - very rare
     */
    @Builder.Default
    private boolean unterhaltAnspruch = false;

    /**
     * Question 18: One-time income in last/next 12 months?
     * Default NEIN
     */
    @Builder.Default
    private boolean einmaligeEinnahmen = false;

    /**
     * Question 19: Income changes expected in next 12 months?
     * Default NEIN (rent increases are known by Wohngeldbehörde)
     */
    @Builder.Default
    private boolean einnahmenAenderung = false;

    /**
     * Question 20: Assets over 60,000 EUR threshold?
     * Default NEIN
     * Exception: Schmerzensgeld doesn't count toward limit
     */
    @Builder.Default
    private boolean vermoegen = false;

    /**
     * If vermoegen but it's Schmerzensgeld (doesn't count)
     */
    @Builder.Default
    private boolean vermoegenIstSchmerzensgeld = false;

    /**
     * Question 27: Will rent change in next 12 months?
     * Default NEIN
     */
    @Builder.Default
    private boolean mietaenderungErwartet = false;

    /**
     * Question 31: Consent for bank statements to be filed
     * Usually JA (makes processing easier)
     */
    @Builder.Default
    private boolean einverstaendnisKontoauszuege = true;
}
