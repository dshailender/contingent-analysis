import { Injectable, inject, signal, computed } from '@angular/core';
import { ValuationApiService } from './valuation-api.service';
import {
  ValuationRequest,
  ValuationResponse,
  SecurityInput,
  HoldingInput
} from '../models/valuation.models';

@Injectable({
  providedIn: 'root'
})
export class ValuationStateService {
  private readonly api = inject(ValuationApiService);

  readonly request = signal<ValuationRequest | null>(null);
  readonly response = signal<ValuationResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly validationIssues = signal<string[]>([]);
  readonly activeTab = signal<'inputs' | 'cap_tables' | 'opm' | 'holdings' | 'exhibits' | 'capital_iq'>('inputs');

  // Computed helpers
  readonly currencySymbol = computed(() => {
    const cur = this.request()?.report_currency || 'EUR';
    return cur === 'EUR' ? '€' : (cur === 'USD' ? '$' : '£');
  });

  readonly availablePreferredSecurities = computed(() => {
    const req = this.request();
    if (!req) return [];
    return req.calibration_securities
      .filter(s => s.security_subtype === 'Preferred Stock')
      .map(s => s.security);
  });

  readonly availableValuationSecurities = computed(() => {
    const req = this.request();
    if (!req) return [];
    return req.valuation_securities.map(s => s.security);
  });

  constructor() {
    this.loadDefaultScenario();
  }

  loadDefaultScenario(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getDefaultScenario().subscribe({
      next: (defReq) => {
        this.request.set(defReq);
        this.calculate();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Failed to load default scenario: ' + (err.error?.detail || err.message));
      }
    });
  }

  calculate(): void {
    const req = this.request();
    if (!req) return;

    this.loading.set(true);
    this.error.set(null);
    this.api.calculateModel(req).subscribe({
      next: (res) => {
        this.response.set(res);
        this.validationIssues.set([]);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        const detail = err.error?.detail || err.message || 'Calculation failed';
        this.error.set(detail);
        if (typeof detail === 'string' && detail.includes(';')) {
          this.validationIssues.set(detail.split(';').map(s => s.trim()));
        }
      }
    });
  }

  validate(): void {
    const req = this.request();
    if (!req) return;
    this.api.validateModel(req).subscribe({
      next: (res) => {
        this.validationIssues.set(res.issues);
      }
    });
  }

  copyCalibrationToValuation(): void {
    const req = this.request();
    if (!req) return;
    const cloned = JSON.parse(JSON.stringify(req.calibration_securities));
    this.request.update(r => r ? { ...r, valuation_securities: cloned } : null);
    this.calculate();
  }

  addSecurityRow(target: 'calibration' | 'valuation'): void {
    const newRow: SecurityInput = {
      security: 'New Security',
      security_subtype: 'Preferred Stock',
      shares: 1000,
      exercise_price: null,
      original_issue_price: 1000,
      conversion_price: 1000,
      liquidation_multiplier: 1.0,
      participation: 'No',
      max_participation_cap: 'NA',
      seniority: 1,
      issue_date: this.request()?.calibration_date || '2025-02-26',
      dividend_rate: 0.0,
      compounding_convention: 'Annual',
      dividends_paid_to_date: 0.0
    };

    this.request.update(r => {
      if (!r) return null;
      if (target === 'calibration') {
        return { ...r, calibration_securities: [...r.calibration_securities, newRow] };
      } else {
        return { ...r, valuation_securities: [...r.valuation_securities, newRow] };
      }
    });
  }

  removeSecurityRow(target: 'calibration' | 'valuation', index: number): void {
    this.request.update(r => {
      if (!r) return null;
      if (target === 'calibration') {
        const rows = [...r.calibration_securities];
        rows.splice(index, 1);
        return { ...r, calibration_securities: rows };
      } else {
        const rows = [...r.valuation_securities];
        rows.splice(index, 1);
        return { ...r, valuation_securities: rows };
      }
    });
    this.calculate();
  }

  addHoldingRow(): void {
    const newH: HoldingInput = {
      fund: 'S2G Fund I',
      security: this.availableValuationSecurities()[0] || 'Common Stock',
      units: 100,
      cost: 100000
    };
    this.request.update(r => r ? { ...r, holdings: [...r.holdings, newH] } : null);
    this.calculate();
  }

  removeHoldingRow(index: number): void {
    this.request.update(r => {
      if (!r) return null;
      const h = [...r.holdings];
      h.splice(index, 1);
      return { ...r, holdings: h };
    });
    this.calculate();
  }

  setFirmLogo(base64: string): void {
    this.request.update(r => r ? { ...r, firm_logo_base64: base64 } : null);
    this.calculate();
  }

  removeFirmLogo(): void {
    this.request.update(r => r ? { ...r, firm_logo_base64: undefined } : null);
    this.calculate();
  }

  setSecondaryCurrency(show: boolean, currency: string, fxRate: number): void {
    this.request.update(r => r ? {
      ...r,
      show_secondary_currency: show,
      secondary_currency: currency,
      secondary_fx_rate: fxRate
    } : null);
    this.calculate();
  }

  refreshRiskFreeCurves(): void {
    const req = this.request();
    if (!req) return;
    this.loading.set(true);
    // Refresh curves via API
    this.api.getRiskFreeCurves().subscribe({
      next: (curves) => {
        // Trigger calculate to get fresh interpolated official curves
        this.calculate();
      },
      error: () => {
        this.calculate();
      }
    });
  }

  downloadExcel(): void {
    const req = this.request();
    if (!req) return;
    this.loading.set(true);
    this.api.exportExcel(req).subscribe({
      next: (blob) => {
        this.loading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${req.company_name}_Valuation_Report.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Failed to download Excel report: ' + (err.error?.detail || err.message));
      }
    });
  }

  downloadPdf(): void {
    const req = this.request();
    if (!req) return;
    this.loading.set(true);
    this.api.exportPdf(req).subscribe({
      next: (blob) => {
        this.loading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${req.company_name}_Valuation_Report.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Failed to download PDF report: ' + (err.error?.detail || err.message));
      }
    });
  }
}

