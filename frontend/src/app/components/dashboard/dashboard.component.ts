import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { DashboardResponse, Frist } from '../../models/klient.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  dashboard: DashboardResponse | null = null;
  faelligeFristen: Frist[] = [];
  loading = true;
  error: string | null = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    console.log('[Dashboard] Component initialized');
    this.loadDashboard();
  }

  loadDashboard() {
    console.log('[Dashboard] Loading dashboard...');
    this.loading = true;
    this.error = null;

    this.api.getDashboard().subscribe({
      next: (data) => {
        console.log('[Dashboard] Dashboard loaded:', data);
        console.log('[Dashboard] Data type:', typeof data);
        console.log('[Dashboard] Data keys:', data ? Object.keys(data) : 'null');
        this.dashboard = data;
        console.log('[Dashboard] this.dashboard set to:', this.dashboard);
        console.log('[Dashboard] Setting loading = false');
        this.loading = false;
        this.cdr.detectChanges();
        this.loadFaelligeFristen();
      },
      error: (err) => {
        console.error('[Dashboard] Error loading dashboard:', err);
        this.error = `Dashboard konnte nicht geladen werden: ${err.status} ${err.statusText || err.message}`;
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadFaelligeFristen() {
    console.log('[Dashboard] Loading fristen...');
    this.api.getFaelligeFristen(14).subscribe({
      next: (fristen) => {
        console.log('[Dashboard] Fristen loaded:', fristen);
        this.faelligeFristen = fristen || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Dashboard] Error loading fristen:', err);
        this.faelligeFristen = [];
        this.cdr.detectChanges();
      },
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'UEBERFAELLIG':
        return 'status-danger';
      case 'ERINNERUNG':
        return 'status-warning';
      case 'IN_BEARBEITUNG':
        return 'status-info';
      case 'ERLEDIGT':
        return 'status-success';
      default:
        return 'status-default';
    }
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OFFEN: 'Offen',
      ERINNERUNG: 'Erinnerung',
      IN_BEARBEITUNG: 'In Bearbeitung',
      ERLEDIGT: 'Erledigt',
      UEBERFAELLIG: 'Überfällig',
    };
    return labels[status] || status;
  }

  getTypLabel(typ: string): string {
    const labels: Record<string, string> = {
      WOHNGELD_ERSTANTRAG: 'Erstantrag',
      WOHNGELD_WEITERBEWILLIGUNG: 'Weiterbewilligung',
      WOHNGELD_ERHOEHUNG: 'Erhöhung',
      DOKUMENT_NACHREICHEN: 'Dokument',
      SONSTIGE: 'Sonstige',
    };
    return labels[typ] || typ;
  }
}
