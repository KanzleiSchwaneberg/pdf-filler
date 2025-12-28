import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Klient } from '../../models/klient.model';

@Component({
  selector: 'app-klient-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './klient-list.component.html',
  styleUrl: './klient-list.component.scss',
})
export class KlientListComponent implements OnInit {
  klienten: Klient[] = [];
  filteredKlienten: Klient[] = [];
  searchQuery = '';
  loading = true;
  error: string | null = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    console.log('[KlientList] Component initialized');
    this.loadKlienten();
  }

  loadKlienten() {
    console.log('[KlientList] Loading klienten...');
    this.loading = true;
    this.error = null;

    this.api.getKlienten().subscribe({
      next: (data) => {
        console.log('[KlientList] Klienten loaded:', data);
        this.klienten = data || [];
        this.filteredKlienten = data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[KlientList] Error loading klienten:', err);
        this.error = `Klienten konnten nicht geladen werden: ${err.status} ${err.statusText || err.message}`;
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSearch() {
    if (!this.searchQuery.trim()) {
      this.filteredKlienten = this.klienten;
      return;
    }

    const query = this.searchQuery.toLowerCase();
    this.filteredKlienten = this.klienten.filter(
      (k) =>
        k.familienname.toLowerCase().includes(query) ||
        k.vorname.toLowerCase().includes(query) ||
        k.ort?.toLowerCase().includes(query)
    );
  }

  deleteKlient(klient: Klient) {
    if (!confirm(`Klient "${klient.vorname} ${klient.familienname}" wirklich deaktivieren?`)) {
      return;
    }

    this.api.deleteKlient(klient.id!).subscribe({
      next: () => {
        this.loadKlienten();
      },
      error: (err) => {
        this.error = 'Klient konnte nicht deaktiviert werden';
        console.error(err);
        this.cdr.detectChanges();
      },
    });
  }

  getOffeneFristen(klient: Klient): number {
    return klient.fristen?.filter((f) => f.status !== 'ERLEDIGT').length || 0;
  }

  hasUeberfaelligeFristen(klient: Klient): boolean {
    return klient.fristen?.some((f) => f.status === 'UEBERFAELLIG') || false;
  }
}
