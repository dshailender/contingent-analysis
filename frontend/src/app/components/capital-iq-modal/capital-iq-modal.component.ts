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
          <h2>S&amp;P Capital IQ Volatility Bridge &amp; Merton Delevering</h2>
          <p class="section-desc">
            Bridge your model with S&amp;P Capital IQ Excel Desktop. Download the formula template, refresh market cap and debt in Excel, and upload to calculate log-return equity volatility and Merton distance-to-default unlevered asset volatility.
          </p>
        </div>

        <div class="action-row">
          <button class="btn" (click)="downloadTemplate()" [disabled]="downloading()" aria-label="Download Excel bridge template">
            <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5z"/>
              <path d="M7.646 11.854a.5.5 0 0 0 .708 0l3-3a.5.5 0 0 0-.708-.708L8.5 10.293V1.5a.5.5 0 0 0-1 0v8.793L5.354 8.146a.5.5 0 1 0-.708.708l3 3z"/>
            </svg>
            <span>Download Bridge Template (.xlsx)</span>
          </button>
        </div>
      </div>

      <!-- Upload Dropzone -->
      <div 
        class="upload-box" 
        [class.dragover]="isDragOver()" 
        (dragover)="onDragOver($event)" 
        (dragleave)="isDragOver.set(false)" 
        (drop)="onDrop($event)"
        role="region"
        aria-label="Capital IQ Excel upload dropzone">
        <input 
          type="file" 
          #fileInput 
          (change)="onFileSelected($event)" 
          accept=".xlsx,.xlsm" 
          style="display: none;" 
          aria-label="Upload refreshed Capital IQ workbook" />
        <div class="upload-content">
          <svg class="upload-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="12" y1="18" x2="12" y2="12"></line>
            <line x1="9" y1="15" x2="15" y2="15"></line>
          </svg>
          <p class="upload-title">Drop your refreshed Capital IQ Excel file here</p>
          <p class="upload-subtitle">Accepts .xlsx or .xlsm refreshed with S&amp;P Capital IQ Office ribbon</p>
          <button class="btn secondary" (click)="fileInput.click()" type="button">
            Browse Workbook File
          </button>
        </div>
      </div>

      @if (uploadError()) {
        <div class="alert-box" role="alert">
          <strong>⚠️ Upload Error:</strong> {{ uploadError() }}
        </div>
      }

      <!-- Parsed Volatility Results -->
      @if (parsedResult(); as res) {
        <div class="parsed-results-panel">
          <div class="results-header">
            <h3>Guideline Public Company (GPC) Peer Group Analysis</h3>
            <span class="as-of-badge">As of: {{ res.as_of_date }}</span>
          </div>

          <div class="spreadsheet-container" tabindex="0" role="region" aria-label="GPC Peer group volatility table">
            <table class="spreadsheet-table">
              <thead>
                <tr>
                  <th scope="col" style="width: 14%;">Ticker / ID</th>
                  <th scope="col" style="width: 26%;">Company Name</th>
                  <th scope="col" style="width: 12%; text-align: right;">Market Cap (mm)</th>
                  <th scope="col" style="width: 12%; text-align: right;">Total Debt (mm)</th>
                  <th scope="col" style="width: 12%; text-align: right;">D/E Ratio</th>
                  <th scope="col" style="width: 12%; text-align: right;">Equity Volatility</th>
                  <th scope="col" style="width: 12%; text-align: right;">Merton Asset Vol</th>
                </tr>
              </thead>
              <tbody>
                @for (p of res.peers; track p.ticker) {
                  <tr>
                    <td><strong>{{ p.ticker }}</strong></td>
                    <td>{{ p.company_name }}</td>
                    <td class="text-right tabular">{{ p.market_cap | number:'1.1-1' }}</td>
                    <td class="text-right tabular">{{ p.total_debt | number:'1.1-1' }}</td>
                    <td class="text-right tabular">{{ p.debt_to_equity | number:'1.2-2' }}x</td>
                    <td class="text-right tabular">{{ (p.equity_volatility * 100) | number:'1.1-1' }}%</td>
                    <td class="text-right tabular"><strong>{{ (p.merton_asset_volatility * 100) | number:'1.1-1' }}%</strong></td>
                  </tr>
                }
                <!-- Summary Stats -->
                <tr class="total-row">
                  <td colspan="5"><strong>Median Peer Volatility Metrics</strong></td>
                  <td class="text-right tabular">{{ (res.raw_equity_volatility_stats.median * 100) | number:'1.1-1' }}%</td>
                  <td class="text-right tabular"><strong>{{ (res.merton_asset_volatility_stats.median * 100) | number:'1.1-1' }}%</strong></td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Apply Volatility Action -->
          <div class="apply-vol-box">
            <div>
              <span class="vol-label">Selected Subject Asset Volatility:</span>
              <span class="vol-val tabular">{{ (res.selected_asset_volatility * 100) | number:'1.1-1' }}%</span>
              <span class="vol-sub">(Relevered Subject Equity Volatility: {{ (res.concluded_relevered_equity_volatility * 100) | number:'1.1-1' }}%)</span>
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

    .btn-icon {
      width: 13px;
      height: 13px;
    }

    .upload-box {
      border: 2px dashed #cbdad6;
      border-radius: 10px;
      padding: 30px 20px;
      text-align: center;
      background: #fbfdfc;
      transition: all 0.14s ease;
      margin-bottom: 18px;
    }

    .upload-box.dragover {
      border-color: var(--teal);
      background: #eef7f5;
    }

    .upload-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .upload-icon-svg {
      width: 38px;
      height: 38px;
      color: var(--teal);
      stroke: var(--teal);
      margin-bottom: 4px;
    }

    .upload-title {
      font-size: 14px;
      font-weight: 700;
      color: #1e2e33;
    }

    .upload-subtitle {
      font-size: 12px;
      color: #64757c;
      margin-bottom: 6px;
    }

    .parsed-results-panel {
      margin-top: 20px;
    }

    .results-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .as-of-badge {
      font-size: 11px;
      color: #607279;
      font-style: italic;
    }

    .text-right {
      text-align: right;
    }

    .apply-vol-box {
      margin-top: 14px;
      padding: 14px 18px;
      background: #f0f7f5;
      border: 1px solid #c8dfd8;
      border-radius: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .vol-label {
      font-size: 12px;
      color: #4b5d63;
      margin-right: 8px;
    }

    .vol-val {
      font-size: 16px;
      font-weight: 800;
      color: var(--teal);
      margin-right: 8px;
    }

    .vol-sub {
      font-size: 11.5px;
      color: #6a7c82;
    }
  `]
})
export class CapitalIqModalComponent {
  private readonly api = inject(ValuationApiService);
  readonly state = inject(ValuationStateService);

  readonly isDragOver = signal<boolean>(false);
  readonly uploading = signal<boolean>(false);
  readonly downloading = signal<boolean>(false);
  readonly uploadError = signal<string | null>(null);
  readonly parsedResult = signal<VolatilityAnalysisResult | null>(null);

  downloadTemplate(): void {
    const req = this.state.request();
    if (!req) return;

    this.downloading.set(true);
    const payload = {
      tickers: ['IQ247543', 'IQ28472', 'IQ113429'],
      calibration_date: req.calibration_date,
      valuation_date: req.valuation_date,
      currency: req.report_currency
    };

    this.api.downloadCapitalIqWorkbook(payload).subscribe({
      next: (blob) => {
        this.downloading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `CapitalIQ_Volatility_Bridge_${req.company_name}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.downloading.set(false);
        this.uploadError.set('Failed to generate template: ' + (err.error?.detail || err.message));
      }
    });
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    this.isDragOver.set(true);
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.isDragOver.set(false);
    if (e.dataTransfer && e.dataTransfer.files.length > 0) {
      this.handleFile(e.dataTransfer.files[0]);
    }
  }

  onFileSelected(e: Event): void {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  private handleFile(file: File): void {
    this.uploading.set(true);
    this.uploadError.set(null);

    this.api.uploadCapitalIqWorkbook(file).subscribe({
      next: (res) => {
        this.uploading.set(false);
        this.parsedResult.set(res);
      },
      error: (err) => {
        this.uploading.set(false);
        this.uploadError.set('Upload failed: ' + (err.error?.detail || err.message));
      }
    });
  }

  applyConcludedVolatility(releveredVolPct: number): void {
    this.state.request.update(r => r ? { ...r, vol_valuation: releveredVolPct } : null);
    this.state.calculate();
  }
}
