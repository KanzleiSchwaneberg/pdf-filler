import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap, catchError, throwError } from 'rxjs';
import {
  Klient,
  Frist,
  FristTyp,
  FristStatus,
  VollstaendigkeitResult,
  DashboardResponse,
  ApiResponse,
} from '../models/klient.model';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  // Direct backend URL (bypasses proxy)
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {
    console.log('[ApiService] Initialized with baseUrl:', this.baseUrl);
  }

  private logRequest(method: string, url: string) {
    console.log(`[ApiService] ${method} ${url}`);
  }

  private handleError(operation: string) {
    return (error: any) => {
      console.error(`[ApiService] ${operation} failed:`, error);
      console.error('[ApiService] Status:', error.status);
      console.error('[ApiService] Message:', error.message);
      if (error.error) {
        console.error('[ApiService] Error body:', error.error);
      }
      return throwError(() => error);
    };
  }

  // ==================== Dashboard ====================

  getDashboard(): Observable<DashboardResponse> {
    const url = `${this.baseUrl}/dashboard`;
    this.logRequest('GET', url);
    return this.http
      .get<ApiResponse<DashboardResponse>>(url)
      .pipe(
        tap((res) => console.log('[ApiService] getDashboard raw response:', res)),
        map((res) => {
          // Handle both wrapped and unwrapped responses
          if (res && 'data' in res) {
            return res.data;
          }
          return res as unknown as DashboardResponse;
        }),
        catchError(this.handleError('getDashboard'))
      );
  }

  getFaelligeFristen(tage: number = 30): Observable<Frist[]> {
    const url = `${this.baseUrl}/dashboard/faellige-fristen?tage=${tage}`;
    this.logRequest('GET', url);
    return this.http
      .get<ApiResponse<Frist[]>>(url)
      .pipe(
        tap((res) => console.log('[ApiService] getFaelligeFristen raw response:', res)),
        map((res) => {
          if (res && 'data' in res) {
            return res.data || [];
          }
          return (res as unknown as Frist[]) || [];
        }),
        catchError(this.handleError('getFaelligeFristen'))
      );
  }

  // ==================== Klienten ====================

  getKlienten(): Observable<Klient[]> {
    const url = `${this.baseUrl}/klienten`;
    this.logRequest('GET', url);
    return this.http
      .get<ApiResponse<Klient[]>>(url)
      .pipe(
        tap((res) => console.log('[ApiService] getKlienten raw response:', res)),
        map((res) => {
          if (res && 'data' in res) {
            return res.data || [];
          }
          return (res as unknown as Klient[]) || [];
        }),
        catchError(this.handleError('getKlienten'))
      );
  }

  getKlient(id: number): Observable<Klient> {
    return this.http
      .get<ApiResponse<Klient>>(`${this.baseUrl}/klienten/${id}`)
      .pipe(map((res) => res.data));
  }

  searchKlienten(query: string): Observable<Klient[]> {
    return this.http
      .get<ApiResponse<Klient[]>>(`${this.baseUrl}/klienten/search?q=${query}`)
      .pipe(map((res) => res.data));
  }

  createKlient(klient: Klient): Observable<Klient> {
    return this.http
      .post<ApiResponse<Klient>>(`${this.baseUrl}/klienten`, klient)
      .pipe(map((res) => res.data));
  }

  updateKlient(id: number, klient: Klient): Observable<Klient> {
    return this.http
      .put<ApiResponse<Klient>>(`${this.baseUrl}/klienten/${id}`, klient)
      .pipe(map((res) => res.data));
  }

  deleteKlient(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/klienten/${id}`)
      .pipe(map((res) => res.data));
  }

  checkVollstaendigkeit(id: number): Observable<VollstaendigkeitResult> {
    return this.http
      .get<ApiResponse<VollstaendigkeitResult>>(
        `${this.baseUrl}/klienten/${id}/check`
      )
      .pipe(map((res) => res.data));
  }

  generateEntwurf(
    id: number,
    erstantrag: boolean = true
  ): Observable<{ outputPath: string; filename: string; fieldsFound: number; fieldsFilled: number }> {
    return this.http
      .post<ApiResponse<{ outputPath: string; filename: string; fieldsFound: number; fieldsFilled: number }>>(
        `${this.baseUrl}/klienten/${id}/entwurf?erstantrag=${erstantrag}`,
        {}
      )
      .pipe(map((res) => res.data));
  }

  downloadPdf(path: string): void {
    const url = `${this.baseUrl}/pdf/download?path=${encodeURIComponent(path)}`;
    window.open(url, '_blank');
  }

  // ==================== Fristen ====================

  getFristenByKlient(klientId: number): Observable<Frist[]> {
    return this.http
      .get<ApiResponse<Frist[]>>(`${this.baseUrl}/fristen/klient/${klientId}`)
      .pipe(map((res) => res.data));
  }

  createFrist(
    klientId: number,
    typ: FristTyp,
    faelligAm: string,
    beschreibung?: string
  ): Observable<Frist> {
    const params = new URLSearchParams({
      klientId: klientId.toString(),
      typ,
      faelligAm,
    });
    if (beschreibung) params.append('beschreibung', beschreibung);

    return this.http
      .post<ApiResponse<Frist>>(`${this.baseUrl}/fristen?${params}`, {})
      .pipe(map((res) => res.data));
  }

  updateFristStatus(id: number, status: FristStatus): Observable<Frist> {
    return this.http
      .patch<ApiResponse<Frist>>(
        `${this.baseUrl}/fristen/${id}/status?status=${status}`,
        {}
      )
      .pipe(map((res) => res.data));
  }

  erledigeFrist(
    id: number,
    folgefristErstellen: boolean = true
  ): Observable<Frist> {
    return this.http
      .post<ApiResponse<Frist>>(
        `${this.baseUrl}/fristen/${id}/erledigen?folgefristErstellen=${folgefristErstellen}`,
        {}
      )
      .pipe(map((res) => res.data));
  }

  deleteFrist(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/fristen/${id}`)
      .pipe(map((res) => res.data));
  }

  getUeberfaelligeFristen(): Observable<Frist[]> {
    return this.http
      .get<ApiResponse<Frist[]>>(`${this.baseUrl}/fristen/ueberfaellig`)
      .pipe(map((res) => res.data));
  }
}
