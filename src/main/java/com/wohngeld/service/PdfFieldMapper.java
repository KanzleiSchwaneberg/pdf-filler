package com.wohngeld.service;

import com.wohngeld.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Direct field mapping for the Wohngeld PDF form.
 * Maps exact PDF field names to values from the request.
 *
 * Field naming convention:
 * - MZ1.3-CB_ = Checkbox
 * - MZ1.3-ET_ = Text field (Eingabetext)
 * - MZ1.3-DA_ = Date field
 * - MZ1.3-AN_ = IBAN character fields
 * - MZ1.3-MTF_ = Multi-line text field
 */
@Component
@Slf4j
public class PdfFieldMapper {

    /**
     * Creates a complete field mapping from the request data.
     * Returns a map of PDF field name -> value to set.
     */
    public Map<String, Object> createFieldMapping(WohngeldAntragRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();

        mapAntragsdaten(fields, request.getAntrag());
        mapAntragsteller(fields, request.getAntragsteller());
        mapAdresse(fields, request.getAdresse());
        mapWohnung(fields, request.getWohnung());
        mapMiete(fields, request.getMiete());
        mapEinkommen(fields, request.getAntragsteller(), request.getEinkommen());
        mapBankverbindung(fields, request.getBankverbindung(), request.getAntragsteller(), request.getAdresse());
        mapZusatzfragen(fields, request);

        return fields;
    }

    private void mapAntragsdaten(Map<String, Object> fields, AntragDaten antrag) {
        if (antrag == null) return;

        // Application type checkboxes
        boolean erstantrag = Boolean.TRUE.equals(antrag.getErstantrag());
        fields.put("MZ1.3-CB_AllgAntragstyp_Erstantrag", erstantrag);
        fields.put("MZ1.3-CB_AllgAntragstyp_Weiterleistungsantrag", !erstantrag);

        // Wohngeld number (only for Weiterleistungsantrag)
        if (!erstantrag && antrag.getWohngeldnummer() != null) {
            fields.put("MZ1.3-MTF_AllgWoGNR_AKZ", antrag.getWohngeldnummer());
        }
    }

    private void mapAntragsteller(Map<String, Object> fields, Antragsteller a) {
        if (a == null) return;

        // Personal information
        fields.put("MZ1.3-ET_PersAngFamilienname", a.getNachname());
        fields.put("MZ1.3-ET_PersAngVornamen", a.getVorname());
        fields.put("MZ1.3-DA_PersAngGeburtsdatum", a.getGeburtsdatum());
        fields.put("MZ1.3-ET_PersAngGeburtsort", a.getGeburtsort());
        fields.put("MZ1.3-ET_PersAngGeburtsname", a.getGeburtsname());
        fields.put("MZ1.3-ET_PersAngStaatsangehörigkeit", a.getStaatsangehoerigkeit());
        fields.put("MZ1.3-ET_PersAngTelefonnummer", a.getTelefon());
        fields.put("MZ1.3-ET_PersAngE-Mail", a.getEmail());

        // Gender checkboxes
        String geschlecht = normalizeValue(a.getGeschlecht());
        fields.put("MZ1.3-CB_PersAngGeschlechtMännlich", "maennlich".equals(geschlecht) || "männlich".equals(geschlecht));
        fields.put("MZ1.3-CB_PersAngGeschlechtWeiblich", "weiblich".equals(geschlecht));
        fields.put("MZ1.3-CB_PersAngGeschlechtDivers", "divers".equals(geschlecht));
        fields.put("MZ1.3-CB_PersAngGeschlechtKeineAngabe", "keineangabe".equals(geschlecht));

        // Marital status checkboxes
        String famstand = normalizeValue(a.getFamilienstand());
        fields.put("MZ1.3-CB_PersAngFamStandledig", "ledig".equals(famstand));
        fields.put("MZ1.3-CB_PersAngFamStandverheiratet", "verheiratet".equals(famstand));
        fields.put("MZ1.3-CB_PersAngFamStandgetrenntlebend", famstand != null && famstand.contains("getrennt"));
        fields.put("MZ1.3-CB_PersAngFamStandeingLebenspartner", famstand != null && famstand.contains("lebenspartner"));
        fields.put("MZ1.3-CB_PersAngFamStandgeschieden", "geschieden".equals(famstand));
        fields.put("MZ1.3-CB_PersAngFamStandverwitwet", "verwitwet".equals(famstand));
        fields.put("MZ1.3-CB_PersAngFamStandnichtehelicheLebenspartner", famstand != null && famstand.contains("nichtehelich"));

        // Employment status checkboxes
        String erwerbsstatus = normalizeValue(a.getErwerbsstatus());
        fields.put("MZ1.3-CB_PersAngErwerbArbeitnehmer", "erwerbstaetig".equals(erwerbsstatus) || "arbeitnehmer".equals(erwerbsstatus));
        fields.put("MZ1.3-CB_PersAngErwerbSelbständiger", erwerbsstatus != null && erwerbsstatus.contains("selbst"));
        fields.put("MZ1.3-CB_PersAngErwerbAzubi", "azubi".equals(erwerbsstatus) || "schueler".equals(erwerbsstatus) || "student".equals(erwerbsstatus));
        fields.put("MZ1.3-CB_PersAngErwerbRentner", "rentner".equals(erwerbsstatus) || "rentnerin".equals(erwerbsstatus));
        fields.put("MZ1.3-CB_PersAngErwerbArbeitslos", "arbeitslos".equals(erwerbsstatus));
        fields.put("MZ1.3-CB_PersAngErwerbNichterwerbsperson", "nichterwerbsperson".equals(erwerbsstatus));
    }

    private void mapAdresse(Map<String, Object> fields, Adresse adr) {
        if (adr == null) return;

        // Current address (Wohnung)
        fields.put("MZ1.3-ET_WohnungAnschriftStraße", adr.getStrasse());
        fields.put("MZ1.3-ET_WohnungAnschriftHausnummer", adr.getHausnummer());
        fields.put("MZ1.3-ET_WohnungAnschriftPostleitzahl", adr.getPlz());
        fields.put("MZ1.3-ET_WohnungAnschriftWohnort", adr.getOrt());
    }

    private void mapWohnung(Map<String, Object> fields, Wohnung w) {
        if (w == null) return;

        // Housing size
        if (w.getWohnflaecheQm() != null) {
            fields.put("MZ1.3-ET_MieteGrößeWohnung", formatNumber(w.getWohnflaecheQm()));
        }

        // Move-in date (Zukunft = future address fields, but also used for move-in)
        if (w.getEinzugsdatum() != null) {
            fields.put("MZ1.3-DA_WohnungZKAnschriftEinzugsdatum", w.getEinzugsdatum());
        }

        // Housing type
        String wohnverhaeltnis = normalizeValue(w.getWohnverhaeltnis());
        fields.put("MZ1.3-CB_IchBinHauptmieter", "hauptmieter".equals(wohnverhaeltnis));
        fields.put("MZ1.3-CB_IchBinUntermieter", "untermieter".equals(wohnverhaeltnis));
        fields.put("MZ1.3-CB_IchBinHeimbewohner", "heimbewohner".equals(wohnverhaeltnis));
        fields.put("MZ1.3-CB_IchBinBewohnerMehr", "eigentum".equals(wohnverhaeltnis) || "eigentuemer".equals(wohnverhaeltnis));

        // Relationship with landlord
        boolean verwandt = Boolean.TRUE.equals(w.getVerwandtschaftMitVermieter());
        fields.put("MZ1.3-CB_IchBinVerwandtVerNein", !verwandt);
        fields.put("MZ1.3-CB_IIchBinVerwandtVerJa", verwandt);

        // Subsidized housing (Mietpreisbindung)
        boolean gefoerdert = Boolean.TRUE.equals(w.getMietpreisbindung());
        fields.put("MZ1.3-CB_WohnungGefördertNein", !gefoerdert);
        fields.put("MZ1.3-CB_WohnungGefördertJa", gefoerdert);
    }

    private void mapMiete(Map<String, Object> fields, Miete m) {
        if (m == null) return;

        // Total rent
        if (m.getGesamtmiete() != null) {
            fields.put("MZ1.3-ET_MieteGesamt", formatCurrency(m.getGesamtmiete()));
        }

        // Heating costs
        boolean hasHeizkosten = m.getHeizkosten() != null && m.getHeizkosten() > 0;
        fields.put("MZ1.3-CB_MonatMieteHeizkostemNein", !hasHeizkosten);
        fields.put("MZ1.3-CB_MonatMieteHeizkostemJa", false); // In rent
        fields.put("MZ1.3-CB_MonatMieteHeizkostemJaGesond", hasHeizkosten); // Separate
        if (hasHeizkosten) {
            fields.put("MZ1.3-ET_MonatMieteHeizkostemBetrag", formatCurrency(m.getHeizkosten()));
        }

        // Hot water costs
        boolean hasWarmwasser = m.getWarmwasser() != null && m.getWarmwasser() > 0;
        fields.put("MZ1.3-CB_MonatMieteWarmwasserNein", !hasWarmwasser);
        fields.put("MZ1.3-CB_MonatMieteWarmwasserJa", false);
        fields.put("MZ1.3-CB_MonatMieteWarmwasserJaGesond", hasWarmwasser);
        if (hasWarmwasser) {
            fields.put("MZ1.3-ET_MonatMieteWarmwasserBetrag", formatCurrency(m.getWarmwasser()));
        }

        // Garage/Parking - typically Nein
        fields.put("MZ1.3-CB_MonatMieteGarageNein", true);
        fields.put("MZ1.3-CB_MonatMieteGarageJa", false);
        fields.put("MZ1.3-CB_MonatMieteGarageJaGesond", false);

        // Service charges - typically Nein
        fields.put("MZ1.3-CB_MonatMieteServiceNein", true);
        fields.put("MZ1.3-CB_MonatMieteServiceJa", false);
        fields.put("MZ1.3-CB_MonatMieteServiceJaGesond", false);

        // Household energy - typically Nein
        fields.put("MZ1.3-CB_MonatMieteHaushaltsenergieNein", true);
        fields.put("MZ1.3-CB_MonatMieteHaushaltsenergieJa", false);
        fields.put("MZ1.3-CB_MonatMieteHaushaltsenergieJaGesond", false);

        // Rent change expected - default Nein
        fields.put("MZ1.3-CB_MieteVerändNein", true);
        fields.put("MZ1.3-CB_MieteVerändJaVerringern", false);
        fields.put("MZ1.3-CB_MieteVerändJaErhöhen", false);

        // Third party payments - default Nein
        fields.put("MZ1.3-CB_MieteDritteNein", true);
        fields.put("MZ1.3-CB_MieteDritteJa", false);
        fields.put("MZ1.3-CB_MieteAnderePersNein", true);
        fields.put("MZ1.3-CB_MieteAnderePersJa", false);
    }

    private void mapEinkommen(Map<String, Object> fields, Antragsteller a, Einkommen e) {
        if (a == null) return;

        // Income section - HHM1 = Household Member 1 = Applicant
        fields.put("MZ1.3-ET_EinnahmeHHM1Familienname", a.getNachname());
        fields.put("MZ1.3-ET_EinnahmeHHM1Vorname", a.getVorname());

        if (e != null) {
            // Income type and amounts (up to 4 entries)
            if (e.getEinnahmen() != null && !e.getEinnahmen().isEmpty()) {
                for (int i = 0; i < Math.min(4, e.getEinnahmen().size()); i++) {
                    Einnahme einnahme = e.getEinnahmen().get(i);
                    int num = i + 1;
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art" + num, einnahme.getArt());
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art" + num + "Brutto", formatCurrency(einnahme.getBruttoBetrag()));
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art" + num + "Turnus", normalizeTurnus(einnahme.getTurnus()));
                }
            } else {
                // Fallback to old model fields
                if (e.getBruttoeinkommenMonatlich() != null && e.getBruttoeinkommenMonatlich() > 0) {
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art1", "Einkommen");
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art1Brutto", formatCurrency(e.getBruttoeinkommenMonatlich()));
                    fields.put("MZ1.3-ET_EinnahmeHHM1Art1Turnus", "monatlich");
                }
            }

            // Deductions checkboxes
            fields.put("MZ1.3-CB_EinnahmeHHM1Steuern", Boolean.TRUE.equals(e.getSteuern()));
            fields.put("MZ1.3-CB_EinnahmeHHM1RVLV", Boolean.TRUE.equals(e.getRentenversicherung()));
            fields.put("MZ1.3-CB_EinnahmeHHM1KV", Boolean.TRUE.equals(e.getKrankenPflegeversicherung()));
        }
    }

    private void mapBankverbindung(Map<String, Object> fields, Bankverbindung b, Antragsteller a, Adresse adr) {
        if (b == null) return;

        // Payment to me checkbox
        fields.put("MZ1.3-CB_ZahlungAnMich", true);
        fields.put("MZ1.3-CB_AuszahlungHHM", false);

        // IBAN - split into individual character fields (MZ1.3-AN_IBAN1 through IBAN33)
        String iban = b.getIban();
        if (iban != null) {
            // Remove spaces
            iban = iban.replaceAll("\\s", "");
            for (int i = 0; i < Math.min(33, iban.length()); i++) {
                fields.put("MZ1.3-AN_IBAN" + (i + 1), String.valueOf(iban.charAt(i)));
            }
        }

        // Bank name
        fields.put("MZ1.3-ET_AuszahlungNameBank", b.getBank());

        // Account holder info
        if (a != null) {
            fields.put("MZ1.3-ET_AuszahlungFamilienname", a.getNachname());
            fields.put("MZ1.3-ET_AuszahlungVorname", a.getVorname());
        }
        if (adr != null) {
            String anschrift = String.format("%s %s, %s %s",
                adr.getStrasse(), adr.getHausnummer(), adr.getPlz(), adr.getOrt());
            fields.put("MZ1.3-ET_AuszahlungAnschrift", anschrift);
        }
    }

    private void mapZusatzfragen(Map<String, Object> fields, WohngeldAntragRequest request) {
        // Question 4: Other apartment receiving Wohngeld - default Nein
        fields.put("MZ1.3-CB_WohnungAndereWohnungNein", true);
        fields.put("MZ1.3-CB_WohnungAndereWohnungJa", false);

        // Question 5: Secondary residence - default Nein
        fields.put("MZ1.3-CB_WohnungZweitwohnsitzNein", true);
        fields.put("MZ1.3-CB_WohnungZweitwohnsitzJa", false);

        // Question 8: Household member deceased - default Nein
        fields.put("MZ1.3-CB_VerändHHMTodNein", true);
        fields.put("MZ1.3-CB_VerändHHMTodJa", false);
        fields.put("MZ1.3-CB_VerändHHMVerstorbenNein", true);
        fields.put("MZ1.3-CB_VerändHHMVerstorbenJa", false);

        // Question 9: Household size change - default Nein
        fields.put("MZ1.3-CB_VerändHHMAnzahlNein", true);
        fields.put("MZ1.3-CB_VerändHHMAnzahlJa", false);

        // Question 10: Transfer payments - default Nein
        fields.put("MZ1.3-CB_TransfLeistungNein", true);
        fields.put("MZ1.3-CB_TransfLeistungJa", false);

        // Question 11: Asked to apply - default Nein
        fields.put("MZ1.3-CB_TransfWohngeldBeantragenNein", true);
        fields.put("MZ1.3-CB_TransfWohngeldBeantragenJa", false);

        // Question 13: Werbungskosten - default Nein
        fields.put("MZ1.3-CB_FreiBWerbNein", true);
        fields.put("MZ1.3-CB_FreiBWerbJa", false);

        // Question 14: Kinderbetreuungskosten - default Nein
        fields.put("MZ1.3-CB_FreiBKinderbetreuNein", true);
        fields.put("MZ1.3-CB_FreiBKinderbetreuJa", false);

        // Question 15: Schwerbehinderung/Pflegegrad - default Nein
        fields.put("MZ1.3-CB_FreiBSchwerBeNein", true);
        fields.put("MZ1.3-CB_FreiBSchwerBeJa", false);

        // Question 16: Unterhalt (maintenance payments) - default Nein
        fields.put("MZ1.3-CB_FreiBUnterhNein", true);
        fields.put("MZ1.3-CB_FreiBUnterhJa", false);

        // Question 17: Unterhalt claims not enforced - default Nein
        fields.put("MZ1.3-CB_SonstEinUnterhNein", true);
        fields.put("MZ1.3-CB_SonstEinUnterhJa", false);

        // Question 18: One-time income - default Nein
        fields.put("MZ1.3-CB_SonstEinEinmNein", true);
        fields.put("MZ1.3-CB_SonstEinEinmJa", false);

        // Question 19: Income changes expected - default Nein
        fields.put("MZ1.3-CB_SonstEinErhNein", true);
        fields.put("MZ1.3-CB_SonstEinErhJaVer", false);
        fields.put("MZ1.3-CB_SonstEinErhJaErh", false);

        // Question 20: Assets over threshold - default Nein
        fields.put("MZ1.3-CB_SonstEinVermögenNein", true);
        fields.put("MZ1.3-CB_SonstEinVermögenJa", false);

        // Question 6: Third party paying costs - default Nein
        fields.put("MZ1.3-CB_DrittStaatKostentragenNein", true);
        fields.put("MZ1.3-CB_DrittStaatKostentragenJa", false);

        // Question 7: Additional persons in household - default Nein
        fields.put("MZ1.3-CB_WeiterePersonenNein", true);
        fields.put("MZ1.3-CB_WeiterePersonenJa", false);

        // Commercial use of living space - default unchecked
        fields.put("MZ1.3-CB_NutzWohnraumBeruflich", false);
        fields.put("MZ1.3-CB_NutzWohnraumAndPersÜberlassen", false);
        fields.put("MZ1.3-CB_NutzWohnraumAndPersEntgeltlich", false);

        // Consent checkbox (Question 31)
        fields.put("MZ1.3-CB_HinweisAbfrage", true);
    }

    // Helper methods

    private String normalizeValue(String value) {
        if (value == null) return null;
        return value.toLowerCase()
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "");
    }

    private String normalizeTurnus(String turnus) {
        if (turnus == null) return "monatlich";
        String t = turnus.toLowerCase();
        if (t.contains("monat")) return "monatlich";
        if (t.contains("jahr") || t.contains("annual")) return "jährlich";
        if (t.contains("tag") || t.contains("daily")) return "täglich";
        return "monatlich";
    }

    private String formatCurrency(Double value) {
        if (value == null) return null;
        // German format with comma as decimal separator
        return String.format("%.2f", value).replace(".", ",");
    }

    private String formatNumber(Double value) {
        if (value == null) return null;
        // For area, use comma as decimal separator
        return String.format("%.2f", value).replace(".", ",");
    }
}
