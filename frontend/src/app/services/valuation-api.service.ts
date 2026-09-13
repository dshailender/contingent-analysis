import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ValuationRequest,
  ValuationResponse,
  ValidationResult,
  RiskFreeRateAnalysis
} from '../models/valuation.models';

@Injectable({
  providedIn: 'root'
})
export class ValuationApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  getDefaultScenario(): Observable<ValuationRequest> {
    return this.http.get<ValuationRequest>(`${this.baseUrl}/scenario/default`);
  }

  validateModel(request: ValuationRequest): Observable<ValidationResult> {
    return this.http.post<ValidationResult>(`${this.baseUrl}/validate`, request);
  }

  calculateModel(request: ValuationRequest): Observable<ValuationResponse> {
    return this.http.post<ValuationResponse>(`${this.baseUrl}/calculate`, request);
  }

  getRiskFreeCurves(): Observable<{ us_treasury: { name: string; years: number }[]; ecb: { name: string; years: number }[] }> {
    return this.http.get<{ us_treasury: { name: string; years: number }[]; ecb: { name: string; years: number }[] }>(
      `${this.baseUrl}/risk-free-rates/curves`
    );
  }

  interpolateRiskFree(payload: {
    as_of_date: string;
    exit_date: string;
    day_count_basis: number;
    source_name: string;
    points: [number, number][];
  }): Observable<RiskFreeRateAnalysis> {
    return this.http.post<RiskFreeRateAnalysis>(`${this.baseUrl}/risk-free-rates/interpolate`, payload);
  }

  downloadCapitalIqWorkbook(payload: {
    tickers: string[];
    calibration_date: string;
    valuation_date: string;
    currency: string;
    frequency?: string;
    lookback_years?: number;
  }): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/capital-iq/workbook`, payload, {
      responseType: 'blob'
    });
  }

  uploadCapitalIqWorkbook(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.baseUrl}/capital-iq/upload`, formData);
  }

  exportExcel(request: ValuationRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/exports/excel`, request, {
      responseType: 'blob'
    });
  }

  exportPdf(request: ValuationRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/exports/pdf`, request, {
      responseType: 'blob'
    });
  }
}

