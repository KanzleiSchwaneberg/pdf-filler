package com.wohngeld.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullProcessRequest {

    @NotNull(message = "Antragsdaten sind erforderlich")
    @Valid
    private WohngeldAntragRequest antragData;

    @Builder.Default
    private boolean sendEmail = false;

    @Email(message = "Ungültige Empfänger-Email")
    private String recipientEmail;
}
