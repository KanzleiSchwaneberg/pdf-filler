import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Klient, VollstaendigkeitResult } from '../../models/klient.model';

@Component({
  selector: 'app-klient-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './klient-form.component.html',
  styleUrl: './klient-form.component.scss',
})
export class KlientFormComponent implements OnInit {
  klient: Klient = this.createEmptyKlient();
  isEditMode = false;
  loading = false;
  saving = false;
  error: string | null = null;
  successMessage: string | null = null;
  vollstaendigkeit: VollstaendigkeitResult | null = null;
  generatingPdf = false;

  geschlechtOptions = [
    { value: 'MAENNLICH', label: 'Männlich' },
    { value: 'WEIBLICH', label: 'Weiblich' },
    { value: 'DIVERS', label: 'Divers' },
  ];

  familienstandOptions = [
    { value: 'LEDIG', label: 'Ledig' },
    { value: 'VERHEIRATET', label: 'Verheiratet' },
    { value: 'GESCHIEDEN', label: 'Geschieden' },
    { value: 'VERWITWET', label: 'Verwitwet' },
    { value: 'GETRENNT_LEBEND', label: 'Getrennt lebend' },
    { value: 'LEBENSPARTNERSCHAFT', label: 'Lebenspartnerschaft' },
  ];

  erwerbsstatusOptions = [
    { value: 'RENTNER', label: 'Rentner' },
    { value: 'ERWERBSTAETIG', label: 'Erwerbstätig' },
    { value: 'ARBEITSLOS', label: 'Arbeitslos' },
    { value: 'SELBSTSTAENDIG', label: 'Selbstständig' },
    { value: 'STUDENT', label: 'Student' },
    { value: 'SONSTIGE', label: 'Sonstige' },
  ];

  wohnverhaeltnisOptions = [
    { value: 'HAUPTMIETER', label: 'Hauptmieter' },
    { value: 'UNTERMIETER', label: 'Untermieter' },
    { value: 'HEIMBEWOHNER', label: 'Heimbewohner' },
    { value: 'EIGENTUM', label: 'Eigentum' },
  ];

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'neu') {
      this.isEditMode = true;
      this.loadKlient(+id);
    }
  }

  createEmptyKlient(): Klient {
    return {
      familienname: '',
      vorname: '',
      geburtsdatum: '',
      geschlecht: 'MAENNLICH',
      familienstand: 'LEDIG',
      erwerbsstatus: 'RENTNER',
      staatsangehoerigkeit: 'deutsch',
      strasse: '',
      hausnummer: '',
      plz: '',
      ort: '',
      wohnflaecheQm: 0,
      wohnverhaeltnis: 'HAUPTMIETER',
      gesamtmiete: 0,
      heizkosten: 0,
      einkommenTurnus: 'MONATLICH',
      zahltKrankenPflegeversicherung: true,
    };
  }

  loadKlient(id: number) {
    this.loading = true;
    this.api.getKlient(id).subscribe({
      next: (klient) => {
        this.klient = klient;
        this.loading = false;
        this.cdr.detectChanges();
        this.checkVollstaendigkeit();
      },
      error: (err) => {
        this.error = 'Klient konnte nicht geladen werden';
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }

  checkVollstaendigkeit() {
    if (!this.klient.id) return;

    this.api.checkVollstaendigkeit(this.klient.id).subscribe({
      next: (result) => {
        this.vollstaendigkeit = result;
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err),
    });
  }

  onSubmit() {
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const operation = this.isEditMode
      ? this.api.updateKlient(this.klient.id!, this.klient)
      : this.api.createKlient(this.klient);

    operation.subscribe({
      next: (saved) => {
        this.klient = saved;
        this.isEditMode = true;
        this.saving = false;
        this.successMessage = 'Klient erfolgreich gespeichert';
        this.cdr.detectChanges();
        this.checkVollstaendigkeit();

        // Bei neuem Klient zur Bearbeiten-Ansicht wechseln
        if (!this.route.snapshot.paramMap.get('id') || this.route.snapshot.paramMap.get('id') === 'neu') {
          this.router.navigate(['/klienten', saved.id]);
        }
      },
      error: (err) => {
        this.error = 'Fehler beim Speichern: ' + (err.error?.message || err.message);
        this.saving = false;
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }

  saveAndGoBack() {
    this.saving = true;
    this.error = null;
    this.successMessage = null;

    const operation = this.isEditMode
      ? this.api.updateKlient(this.klient.id!, this.klient)
      : this.api.createKlient(this.klient);

    operation.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/klienten']);
      },
      error: (err) => {
        this.error = 'Fehler beim Speichern: ' + (err.error?.message || err.message);
        this.saving = false;
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }

  generateEntwurf() {
    if (!this.klient.id || !this.vollstaendigkeit?.vollstaendig) return;

    this.generatingPdf = true;
    this.error = null;
    this.successMessage = null;

    this.api.generateEntwurf(this.klient.id, true).subscribe({
      next: (result) => {
        this.generatingPdf = false;
        this.successMessage = `Antragsentwurf erstellt: ${result.filename} (${result.fieldsFilled}/${result.fieldsFound} Felder ausgefüllt)`;
        this.cdr.detectChanges();
        // Automatisch Download starten
        this.api.downloadPdf(result.outputPath);
      },
      error: (err) => {
        this.generatingPdf = false;
        this.error = 'Fehler bei der PDF-Erstellung: ' + (err.error?.message || err.message);
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }
}
