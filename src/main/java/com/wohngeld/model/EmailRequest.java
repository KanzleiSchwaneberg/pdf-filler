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
public class EmailRequest {

    @NotBlank(message = "Empfänger-Email ist erforderlich")
    @Email(message = "Ungültige Email-Adresse")
    private String recipientEmail;

    @NotBlank(message = "PDF-Pfad ist erforderlich")
    private String pdfPath;

    private WohngeldAntragRequest antragData;
}
