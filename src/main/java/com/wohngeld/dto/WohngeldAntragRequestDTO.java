package com.wohngeld.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Main API DTO for Wohngeld application request.
 * This is the external API representation that clients send.
 *
 * The internal service will map this to domain models for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WohngeldAntragRequestDTO {

    /**
     * Application metadata (type, dates, etc.)
     */
    @NotNull(message = "Antragsmetadaten sind erforderlich")
    @Valid
    private AntragMetadatenDTO antrag;

    /**
     * Applicant personal information.
     */
    @NotNull(message = "Antragsteller ist erforderlich")
    @Valid
    private AntragstellerDTO antragsteller;

    /**
     * Address of the apartment for which Wohngeld is requested.
     */
    @NotNull(message = "Adresse ist erforderlich")
    @Valid
    private AdresseDTO adresse;

    /**
     * Housing/apartment details.
     */
    @NotNull(message = "Wohnungsdaten sind erforderlich")
    @Valid
    private WohnungDTO wohnung;

    /**
     * Rent and cost details.
     */
    @NotNull(message = "Mietdaten sind erforderlich")
    @Valid
    private MieteDTO miete;

    /**
     * Income information.
     */
    @NotNull(message = "Einkommensdaten sind erforderlich")
    @Valid
    private EinkommenDTO einkommen;

    /**
     * Bank account details for Wohngeld payment.
     */
    @NotNull(message = "Bankverbindung ist erforderlich")
    @Valid
    private BankverbindungDTO bankverbindung;

    /**
     * Additional yes/no questions from the form.
     * Optional - defaults will be applied if not provided.
     */
    @Builder.Default
    private ZusatzfragenDTO zusatzfragen = new ZusatzfragenDTO();
}
