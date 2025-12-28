export interface Klient {
  id?: number;

  // Stammdaten
  familienname: string;
  vorname: string;
  geburtsdatum: string;
  staatsangehoerigkeit?: string;
  geschlecht: string;
  familienstand: string;
  erwerbsstatus: string;
  geburtsname?: string;
  geburtsort?: string;
  telefon?: string;
  email?: string;

  // Adresse
  strasse: string;
  hausnummer: string;
  plz: string;
  ort: string;
  bundesland?: string;

  // Wohnung
  wohnflaecheQm: number;
  wohnverhaeltnis?: string;
  verwandtschaftMitVermieter?: boolean;
  mietpreisbindung?: boolean;
  einzugsdatum?: string;
  vermieterName?: string;

  // Miete
  gesamtmiete: number;
  heizkostenEnthalten?: boolean;
  heizkosten?: number;
  warmwasserEnthalten?: boolean;
  warmwasserkosten?: number;

  // Einkommen
  einkommensart?: string;
  einkommenBrutto?: number;
  einkommenTurnus?: string;
  zahltKrankenPflegeversicherung?: boolean;

  // Bank
  iban?: string;
  bankName?: string;
  kontoinhaberName?: string;
  kontoinhaberAnschrift?: string;

  // Zusatz
  schwerbehinderungOderPflege?: boolean;
  pflegegrad?: string;
  wohngeldnummer?: string;
  notizen?: string;
  aktiv?: boolean;

  // Fristen
  fristen?: Frist[];

  // Audit
  erstelltAm?: string;
  aktualisiertAm?: string;
}

export interface Frist {
  id?: number;
  typ: FristTyp;
  faelligAm: string;
  erinnerungAm?: string;
  status: FristStatus;
  beschreibung?: string;
  generierterAntragPfad?: string;
  erstelltAm?: string;
}

export type FristTyp =
  | 'WOHNGELD_ERSTANTRAG'
  | 'WOHNGELD_WEITERBEWILLIGUNG'
  | 'WOHNGELD_ERHOEHUNG'
  | 'DOKUMENT_NACHREICHEN'
  | 'SONSTIGE';

export type FristStatus =
  | 'OFFEN'
  | 'ERINNERUNG'
  | 'IN_BEARBEITUNG'
  | 'ERLEDIGT'
  | 'UEBERFAELLIG';

export interface VollstaendigkeitResult {
  vollstaendig: boolean;
  prozentVollstaendig: number;
  fehlendeFelder: string[];
  warnungen: string[];
}

export interface DashboardResponse {
  aktiveKlienten: number;
  klientenMitUnvollstaendigenDaten: number;
  fristenUeberfaellig: number;
  fristenFaelligHeute: number;
  fristenFaelligDieseWoche: number;
  fristenFaelligDiesenMonat: number;
  erinnerungenOffen: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}
