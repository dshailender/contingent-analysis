import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValuationStateService } from '../../services/valuation-state.service';

@Component({
  selector: 'app-engagement-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state.request(); as req) {
      <div class="card accent-card">
        <h2>Engagement & Modeling Parameters</h2>
        <p class="section-desc">
          Configure engagement metadata, statutory dates, day-count basis conventions, and reporting presentation units.
        </p>

        <div class="settings-grid">
          <!-- Company & Client -->
          <div class="field-group">
            <label>Subject Company Name <span class="req">*</span></label>
            <input type="text" [(ngModel)]="req.company_name" (change)="state.calculate()" placeholder="e.g. TADO" />
          </div>

          <div class="field-group">
            <label>Client Name <span class="req">*</span></label>
            <input type="text" [(ngModel)]="req.client_name" (change)="state.calculate()" placeholder="e.g. S2G Investments" />
          </div>

          <div class="field-group">
            <label>Report Purpose</label>
            <select [(ngModel)]="req.report_purpose" (change)="state.calculate()">
              <option value="Valuation Analysis">Valuation Analysis</option>
              <option value="Financial Reporting (ASC 820 / IFRS 13)">Financial Reporting (ASC 820 / IFRS 13)</option>
              <option value="Tax Compliance (409A / 83b)">Tax Compliance (409A / 83b)</option>
              <option value="Transaction Advisory">Transaction Advisory</option>
              <option value="Other (Specify Below)">Other (Specify Below)</option>
            </select>
          </div>

          @if (req.report_purpose === 'Other (Specify Below)') {
            <div class="field-group">
              <label>Custom Purpose Description</label>
              <input type="text" [(ngModel)]="req.report_purpose_manual" (change)="state.calculate()" />
            </div>
          }

          <div class="field-group">
            <label>Report Status</label>
            <select [(ngModel)]="req.report_status" (change)="state.calculate()">
              <option value="DRAFT - For Discussion Purposes Only">DRAFT - For Discussion Purposes Only</option>
              <option value="PRELIMINARY">PRELIMINARY</option>
              <option value="FINAL">FINAL</option>
            </select>
          </div>

          <!-- Dates -->
          <div class="field-group">
            <label>Calibration Date (Financing Event) <span class="req">*</span></label>
            <input type="date" [(ngModel)]="req.calibration_date" (change)="state.calculate()" />
            <span class="field-hint">Date of observed arm's-length transaction</span>
          </div>

          <div class="field-group">
            <label>Valuation Date (Measurement Date) <span class="req">*</span></label>
            <input type="date" [(ngModel)]="req.valuation_date" (change)="state.calculate()" />
            <span class="field-hint">As-of balance sheet / reporting date</span>
          </div>

          <div class="field-group">
            <label>Global Exit Date (Anticipated Liquidity) <span class="req">*</span></label>
            <input type="date" [(ngModel)]="req.exit_date" (change)="state.calculate()" />
            <span class="field-hint">Maturity horizon for option pricing</span>
          </div>

          <!-- Conventions & Presentation -->
          <div class="field-group">
            <label>Day-Count Basis Convention</label>
            <select [(ngModel)]="req.day_count_basis" (change)="state.calculate()">
              <option [ngValue]="0">US 30/360 (Bond Basis)</option>
              <option [ngValue]="1">Actual/Actual (Excel Standard)</option>
              <option [ngValue]="2">Actual/360 (Money Market)</option>
              <option [ngValue]="3">Actual/365 (Fixed)</option>
              <option [ngValue]="4">European 30/360 (Eurobond)</option>
            </select>
          </div>

          <div class="field-group">
            <label>Reporting Currency</label>
            <select [(ngModel)]="req.report_currency" (change)="state.calculate()">
              <option value="EUR">EUR (€) - Euro</option>
              <option value="USD">USD ($) - US Dollar</option>
              <option value="GBP">GBP (£) - British Pound</option>
            </select>
          </div>

          <div class="field-group">
            <label>Report Display Units</label>
            <select [(ngModel)]="req.display_units" (change)="state.calculate()">
              <option value="actual">Actual Amounts (Single units)</option>
              <option value="thousands">Thousands (000s)</option>
              <option value="millions">Millions (mm)</option>
            </select>
          </div>
        </div>

        <!-- Custom Firm Logo & Secondary Currency Row -->
        <div class="branding-bar">
          <div class="branding-group">
            <label class="branding-title">Firm Brand / Logo</label>
            <div class="logo-controls">
              @if (req.firm_logo_base64) {
                <div class="logo-preview-box">
                  <img [src]="req.firm_logo_base64" alt="Firm Logo Preview" class="logo-img" />
                  <button type="button" class="btn-remove-logo" (click)="state.removeFirmLogo()">Remove Logo</button>
                </div>
              } @else {
                <div class="upload-btn-wrap">
                  <input type="file" id="logoUpload" accept="image/png,image/jpeg,image/svg+xml" (change)="onLogoSelected($event)" style="display: none;" />
                  <label for="logoUpload" class="btn-upload">Upload Firm Logo (PNG / SVG)</label>
                  <span class="field-hint">Embedded dynamically into PDF reports & cover exhibits</span>
                </div>
              }
            </div>
          </div>

          <div class="branding-group">
            <label class="branding-title">Secondary Currency Display</label>
            <div class="sec-curr-controls">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="req.show_secondary_currency" (change)="state.calculate()" />
                <span>Show secondary currency columns</span>
              </label>

              @if (req.show_secondary_currency) {
                <div class="sec-curr-fields">
                  <div class="field-group">
                    <label>Currency</label>
                    <select [(ngModel)]="req.secondary_currency" (change)="state.calculate()">
                      <option value="USD">USD ($)</option>
                      <option value="EUR">EUR (€)</option>
                      <option value="GBP">GBP (£)</option>
                      <option value="CHF">CHF</option>
                      <option value="CAD">CAD ($)</option>
                    </select>
                  </div>
                  <div class="field-group">
                    <label>FX Rate (per {{ req.report_currency }})</label>
                    <input type="number" step="0.0001" [(ngModel)]="req.secondary_fx_rate" (change)="state.calculate()" placeholder="1.0000" />
                  </div>
                </div>
              }
            </div>
          </div>
        </div>

        <!-- Calculated Term Feedback -->
        @if (state.response(); as res) {
          <div class="term-feedback-bar">
            <div class="term-item">
              <span class="term-label">Calibration Term (Years):</span>
              <span class="term-val">{{ res.term_calibration | number:'1.2-2' }} yrs</span>
            </div>
            <div class="term-item">
              <span class="term-label">Valuation Term (Years):</span>
              <span class="term-val">{{ res.term_valuation | number:'1.2-2' }} yrs</span>
            </div>
            <div class="term-item">
              <span class="term-label">Convention Applied:</span>
              <span class="term-val">{{ res.day_count_name }}</span>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .section-desc {
      color: #687386;
      font-size: 13px;
      margin-bottom: 18px;
    }

    .settings-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 5px;
    }

    .field-group label {
      font-size: 12px;
      font-weight: 600;
      color: var(--slate);
    }

    .field-hint {
      font-size: 10.5px;
      color: #7a8693;
    }

    .req {
      color: var(--danger);
    }

    .branding-bar {
      margin-top: 18px;
      padding: 14px 16px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    .branding-title {
      font-size: 12px;
      font-weight: 700;
      color: #1e293b;
      margin-bottom: 8px;
      display: block;
    }

    .btn-upload {
      display: inline-block;
      padding: 7px 14px;
      background: #0f5f91;
      color: #fff;
      font-size: 11.5px;
      font-weight: 600;
      border-radius: 6px;
      cursor: pointer;
    }

    .logo-preview-box {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 8px;
      background: #fff;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
    }

    .logo-img {
      max-height: 40px;
      max-width: 160px;
      object-fit: contain;
    }

    .btn-remove-logo {
      padding: 4px 8px;
      font-size: 11px;
      background: #f1f5f9;
      color: #dc2626;
      border: 1px solid #e2e8f0;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      cursor: pointer;
      color: #334155;
    }

    .sec-curr-fields {
      display: flex;
      gap: 14px;
      margin-top: 8px;
    }

    .term-feedback-bar {
      margin-top: 20px;
      padding: 12px 16px;
      background: #f4f8f7;
      border: 1px solid #d8e5e2;
      border-radius: 8px;
      display: flex;
      gap: 24px;
      flex-wrap: wrap;
    }

    .term-item {
      display: flex;
      gap: 6px;
      font-size: 12.5px;
    }

    .term-label {
      color: #5b6973;
      font-weight: 500;
    }

    .term-val {
      color: var(--teal);
      font-weight: 700;
    }
  `]
})
export class EngagementSettingsComponent {
  readonly state = inject(ValuationStateService);

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = reader.result as string;
        this.state.setFirmLogo(base64);
      };
      reader.readAsDataURL(file);
    }
  }
}


