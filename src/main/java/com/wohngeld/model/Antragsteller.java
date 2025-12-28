package com.wohngeld.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Antragsteller {

    @Builder.Default
    private String anrede = "Herr";

    // Geschlecht: "maennlich", "weiblich", "divers"
    @Builder.Default
    private String geschlecht = "maennlich";

    @NotBlank(message = "Vorname ist erforderlich")
    private String vorname;

    @NotBlank(message = "Nachname ist erforderlich")
    private String nachname;

    @NotBlank(message = "Geburtsdatum ist erforderlich")
    private String geburtsdatum;

    private String geburtsort;

    private String geburtsname;

    @Builder.Default
    private String staatsangehoerigkeit = "deutsch";

    // Familienstand: "ledig", "verheiratet", "geschieden", "verwitwet", "getrennt lebend", "eingetragene Lebenspartnerschaft"
    @Builder.Default
    private String familienstand = "ledig";

    // Erwerbsstatus: "erwerbstaetig", "arbeitslos", "rentner", "student", "schueler", "selbststaendig", "sonstige"
    @Builder.Default
    private String erwerbsstatus = "erwerbstaetig";

    private String telefon;

    @Email(message = "Ungültige Email-Adresse")
    private String email;

    public String getFullName() {
        return vorname + " " + nachname;
    }
}
