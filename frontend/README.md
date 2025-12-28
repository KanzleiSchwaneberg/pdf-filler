# Wohngeld Automation - Frontend

Angular-basierte Benutzeroberfläche zur Verwaltung von Wohngeld-Anträgen.

## Kernfunktionen

### Klientenverwaltung
- **Klienten anlegen** - Erfassung aller relevanten Daten (Stammdaten, Adresse, Wohnung, Miete, Einkommen, Bankverbindung)
- **Klienten bearbeiten** - Änderung bestehender Klientendaten
- **Klienten suchen** - Schnellsuche nach Name oder Ort
- **Klienten deaktivieren** - Soft-Delete von Klienten

### Vollständigkeitsprüfung
- **Fortschrittsanzeige** - Visuelle Darstellung der Datenvollständigkeit in Prozent
- **Fehlende Felder** - Auflistung noch auszufüllender Pflichtfelder
- **100%-Indikator** - Grüne Markierung bei vollständigen Daten

### PDF-Generierung
- **Antragsentwurf erstellen** - Automatisches Ausfüllen des Wohngeld-Antragsformulars
- **Direkter Download** - PDF wird nach Generierung automatisch heruntergeladen
- **Feldstatistik** - Anzeige der ausgefüllten Felder (z.B. "45/120 Felder ausgefüllt")

### Dashboard
- **Übersicht** - Statistiken zu aktiven Klienten und Fristen
- **Fällige Fristen** - Liste anstehender Fristen (14 Tage)
- **Warnungen** - Hervorhebung überfälliger Fristen und unvollständiger Daten

### Fristenverwaltung
- **Fristenübersicht** - Alle Fristen eines Klienten auf einen Blick
- **Status-Tracking** - Offen, In Bearbeitung, Erledigt, Überfällig
- **Fristtypen** - Erstantrag, Weiterbewilligung, Erhöhung, Dokument nachreichen

## Technologie

- **Angular 21** mit Standalone Components
- **TypeScript** für typsichere Entwicklung
- **SCSS** für Styling
- **RxJS** für reaktive Datenströme

## Installation

```bash
cd frontend
npm install
```

## Entwicklung

```bash
npm start
```

Die Anwendung läuft auf `http://localhost:4200`.

## Backend-Verbindung

Das Frontend kommuniziert mit dem Spring Boot Backend auf `http://localhost:8080/api`.

## Projektstruktur

```
src/app/
├── components/
│   ├── dashboard/        # Dashboard mit Statistiken
│   ├── klient-list/      # Klientenliste mit Suche
│   └── klient-form/      # Formular für Klienten (Neu/Bearbeiten)
├── services/
│   └── api.service.ts    # HTTP-Kommunikation mit Backend
├── models/
│   └── klient.model.ts   # TypeScript Interfaces
└── app.routes.ts         # Routing-Konfiguration
```

## Build

```bash
ng build
```

Die Build-Artefakte werden im `dist/` Verzeichnis gespeichert.
