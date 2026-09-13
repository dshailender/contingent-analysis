import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValuationApiService } from '../../services/valuation-api.service';
import { ValuationStateService } from '../../services/valuation-state.service';
import { VolatilityAnalysisResult } from '../../models/valuation.models';

@Component({
  selector: 'app-capital-iq-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card accent-card">
      <div class="header-row">
        <div>
          <h2>S&P Capital IQ Volatility Bridge & Merton Delevering</h2>
          <p class="section-desc">
            Bridge your analysis directly with S&P Capital IQ Excel Desktop. Download the formula template, refresh market data, and upload for automatic log-return volatility and Merton distance-to-default delevering.
          </p>
        </div>

        <div class="action-row">
          <button class="btn" (click)="downloadTemplate()">
            📥 Download Bridge Template (.xlsx)
          </button>
        </div>
      </div>

      <!-- Upload Dropzone -->
      <div class="upload-box" [class.dragover]="isDragOver()" (dragover)="onDragOver($event)" (dragleave)="isDragOver.set(false)" (drop)="onDrop($event)">
        <input type="file" #fileInput (change)="onFileSelected($event)" accept=".xlsx,.xlsm" style="display: none;" />
        <div class="upload-content">
          <span class="upload-icon">📊</span>
          <p class="upload-title">Drop your refreshed Capital IQ Excel file here</p>
          <p class="upload-subtitle">Supports .xlsx or .xlsm refreshed with Capital IQ ribbon</p>
          <button class="btn secondary" (click)="fileInput.click()">
            Browse File
          </button>
        </div>
      </div>

      @if (uploadError()) {
        <div class="alert-box">
          {{ uploadError() }}
        </div>
      }

      <!-- Parsed Volatility Results -->
      @if (parsedResult(); as res) {
        <div class="parsed-results-panel">
          <div class="results-header">
            <h3>Guideline Public Company (GPC) Peer Group Analysis</h3>
            <span class="as-of-badge">As of: {{ res.as_of_date }}</span>
          </div>

          <div class="spreadsheet-container">
            <table class="spreadsheet-table">
              <thead>
                <tr>
                  <th>Ticker / ID</th>
                  <th>Company Name</th>
                  <th style="text-align: right;">Market Cap (mm)</th>
                  <th style="text-align: right;">Total Debt (mm)</th>
                  <th style="text-align: right;">D/E Ratio</th>
                  <th style="text-align: right;">Equity Volatility</th>
                  <th style="text-align: right;">Merton Asset Volatility</th>
                </tr>
              </thead>
              <tbody>
                @for (p of res.peers; track p.ticker) {
                  <tr>
                    <td><strong>{{ p.ticker }}</strong></td>
                    <td>{{ p.company_name }}</td>
                    <td style="text-align: right;">{{ p.market_cap | number:'1.1-1' }}</td>
                    <td style="text-align: right;">{{ p.total_debt | number:'1.1-1' }}</td>
                    <td style="text-align: right;">{{ p.debt_to_equity | number:'1.2-2' }}x</td>
                    <td style="text-align: right;">{{ (p.equity_volatility * 100) | number:'1.1-1' }}%</td>
                    <td style="text-align: right;"><strong>{{ (p.merton_asset_volatility * 100) | number:'1.1-1' }}%</strong></td>
                  </tr>
                }
                <!-- Summary Stats -->
                <tr class="total-row">
                  <td colspan="5"><strong>Median Asset Volatility</strong></td>
                  <td style="text-align: right;">{{ (res.raw_equity_volatility_stats.median * 100) | number:'1.1-1' }}%</td>
                  <td style="text-align: right;"><strong>{{ (res.merton_asset_volatility_stats.median * 100) | number:'1.1-1' }}%</strong></td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Apply Volatility Action -->
          <div class="apply-vol-box">
            <div>
              <span class="vol-label">Selected Subject Asset Volatility:</span>
              <span class="vol-val">{{ (res.selected_asset_volatility * 100) | number:'1.1-1' }}%</span>
              <span class="vol-sub">(Relevered Equity Volatility: {{ (res.concluded_relevered_equity_volatility * 100) | number:'1.1-1' }}%)</span>
            </div>
            <button class="btn" (click)="applyConcludedVolatility(res.concluded_relevered_equity_volatility * 100)">
              Apply to Valuation Volatility
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }

    .section-desc {
      color: #687386;
      font-size: 13px;
    }

    .upload-box {
      border: 2px dashed #b8c9c6;
      border-radius: 12px;
      padding: 32px 20px;
      text-align: center;
      background: #fbfdfc;
      transition: all 0.2s ease;
      cursor: pointer;
      margin-bottom: 18px;
    }

    .upload-box:hover, .upload-box.dragover {
      border-color: var(--teal);
      background: #f2f8f7;
    }

    .upload-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .upload-icon {
      font-size: 36px;
    }

    .upload-title {
      font-size: 14px;
      font-weight: 700;
      color: #24353a;
    }

    .upload-subtitle {
      font-size: 12px;
      color: #7b898d;
    }

    .parsed-results-panel {
      margin-top: 20px;
    }

    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }

    .as-of-badge {
      font-size: 11px;
      background: #eef4f3;
      color: var(--teal);
      padding: 3px 8px;
      border-radius: 6px;
      font-weight: 600;
    }

    .apply-vol-box {
      margin-top: 14px;
      padding: 12px 16px;
      background: #f4f9f8;
      border: 1px solid #d4e4e1;
      border-radius: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .vol-label {
      font-size: 12px;
      font-weight: 600;
      color: #48585d;
    }

    .vol-val {
      font-size: 16px;
      font-weight: 800;
      color: var(--teal);
      margin: 0 8px;
    }

    .vol-sub {
      font-size: 11px;
      color: #728185;
    }
  `]
})
export class CapitalIqModalComponent {
  private readonly api = inject(ValuationApiService);
  readonly state = inject(ValuationStateService);

  readonly isDragOver = signal<boolean>(false);
  readonly uploadError = signal<string | null>(null);
  readonly parsedResult = signal<VolatilityAnalysisResult | null>(null);

  downloadTemplate(): void {
    const req = this.state.request();
    if (!req) return;

    this.state.loading.set(true);
    this.api.downloadCapitalIqWorkbook({
      tickers: ['IQ247543', 'IQ28472', 'IQ385732', 'IQ94821', 'IQ582910'],
      calibration_date: req.calibration_date,
      valuation_date: req.valuation_date,
      currency: req.report_currency
    }).subscribe({
      next: (blob) => {
        this.state.loading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Capital_IQ_Volatility_Bridge.xlsx';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.state.loading.set(false);
        this.uploadError.set('Failed to download template: ' + err.message);
      }
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.processFile(event.dataTransfer.files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFile(input.files[0]);
    }
  }

  processFile(file: File): void {
    this.uploadError.set(null);
    this.state.loading.set(true);

    this.api.uploadCapitalIqWorkbook(file).subscribe({
      next: (data) => {
        this.state.loading.set(false);
        if (data.Valuation) {
          this.parsedResult.set(data.Valuation);
        } else if (data.Calibration) {
          this.parsedResult.set(data.Calibration);
        }
      },
      error: (err) => {
        this.state.loading.set(false);
        this.uploadError.set(err.error?.detail || err.message || 'Failed to parse file');
      }
    });
  }

  applyConcludedVolatility(volPercent: number): void {
    this.state.request.update(r => r ? { ...r, vol_valuation: Math.round(volPercent * 10) / 10 } : null);
    this.state.calculate();
  }
}

