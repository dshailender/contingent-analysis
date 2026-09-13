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
        <h2>Engagement &amp; Statutory Parameters</h2>
        <p class="section-desc">
          Configure statutory valuation dates, day-count basis conventions, institutional reporting units, and custom branding.
        </p>

        <div class="settings-grid">
          <!-- Section 1: Engagement Identity -->
          <div class="settings-section">
            <h3 class="section-header">
              <svg class="section-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1v-1z"/>
                <path d="M9.5 1a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-.5.5h-3a.5.5 0 0 1-.5-.5v-1a.5.5 0 0 1 .5-.5h3zm-3-1A1.5 1.5 0 0 0 5 1.5v1A1.5 1.5 0 0 0 6.5 4h3A1.5 1.5 0 0 0 11 2.5v-1A1.5 1.5 0 0 0 9.5 0h-3z"/>
              </svg>
              <span>Company &amp; Engagement Identity</span>
            </h3>

            <div class="fields-stack">
              <div class="field-group">
                <label for="companyName">Subject Company Name <span class="req">*</span></label>
                <input 
                  id="companyName"
                  type="text" 
                  [(ngModel)]="req.company_name" 
                  (change)="state.calculate()" 
                  placeholder="e.g. StellarAI Technologies, Inc." />
              </div>

              <div class="field-group">
                <label for="clientName">Client / Investor Entity <span class="req">*</span></label>
                <input 
                  id="clientName"
                  type="text" 
                  [(ngModel)]="req.client_name" 
                  (change)="state.calculate()" 
                  placeholder="e.g. S2G Investments" />
              </div>

              <div class="field-group">
                <label for="reportPurpose">Report Purpose</label>
                <select id="reportPurpose" [(ngModel)]="req.report_purpose" (change)="state.calculate()">
                  <option value="Valuation Analysis">Valuation Analysis</option>
                  <option value="Financial Reporting (ASC 820 / IFRS 13)">Financial Reporting (ASC 820 / IFRS 13)</option>
                  <option value="Tax Compliance (409A / 83b)">Tax Compliance (409A / 83b)</option>
                  <option value="Transaction Advisory">Transaction Advisory</option>
                  <option value="Other (Specify Below)">Other (Specify Below)</option>
                </select>
              </div>

              @if (req.report_purpose === 'Other (Specify Below)') {
                <div class="field-group">
                  <label for="reportPurposeManual">Custom Purpose Description</label>
                  <input id="reportPurposeManual" type="text" [(ngModel)]="req.report_purpose_manual" (change)="state.calculate()" />
                </div>
              }

              <div class="field-group">
                <label for="reportStatus">Report Engagement Status</label>
                <select id="reportStatus" [(ngModel)]="req.report_status" (change)="state.calculate()">
                  <option value="DRAFT - For Discussion Purposes Only">DRAFT - For Discussion Purposes Only</option>
                  <option value="PRELIMINARY">PRELIMINARY</option>
                  <option value="FINAL">FINAL</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Section 2: Statutory Dates & Day Count Basis -->
          <div class="settings-section">
            <h3 class="section-header">
              <svg class="section-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5zM1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1z"/>
              </svg>
              <span>Statutory Dates &amp; Day-Count Basis</span>
            </h3>

            <div class="fields-stack">
              <div class="field-group">
                <label for="calDate">Calibration Date (Financing Event) <span class="req">*</span></label>
                <input id="calDate" type="date" [(ngModel)]="req.calibration_date" (change)="state.calculate()" />
                <span class="field-hint">Observed arm's-length round pricing date</span>
              </div>

              <div class="field-group">
                <label for="valDate">Valuation Date (Measurement Date) <span class="req">*</span></label>
                <input id="valDate" type="date" [(ngModel)]="req.valuation_date" (change)="state.calculate()" />
                <span class="field-hint">As-of balance sheet reporting date</span>
              </div>

              <div class="field-group">
                <label for="exitDate">Global Expected Exit Date <span class="req">*</span></label>
                <input id="exitDate" type="date" [(ngModel)]="req.exit_date" (change)="state.calculate()" />
                <span class="field-hint">Expected liquidity horizon for Black-Scholes</span>
              </div>

              <div class="field-group">
                <label for="basis">Day-Count Basis Convention</label>
                <select id="basis" [(ngModel)]="req.day_count_basis" (change)="state.calculate()">
                  <option [ngValue]="0">US 30/360 (Bond Basis)</option>
                  <option [ngValue]="1">Actual/Actual (Excel Standard)</option>
                  <option [ngValue]="2">Actual/360 (Money Market)</option>
                  <option [ngValue]="3">Actual/365 (Fixed)</option>
                  <option [ngValue]="4">European 30/360 (Eurobond)</option>
                </select>
              </div>

              <!-- Term Feedback Chips -->
              @if (state.response(); as res) {
                <div class="term-chips-container">
                  <div class="term-chip">
                    <span class="chip-label">Calibration Term:</span>
                    <span class="chip-val">{{ res.term_calibration | number:'1.2-2' }} yrs</span>
                  </div>
                  <div class="term-chip">
                    <span class="chip-label">Valuation Term:</span>
                    <span class="chip-val">{{ res.term_valuation | number:'1.2-2' }} yrs</span>
                  </div>
                  <div class="term-chip">
                    <span class="chip-label">Basis:</span>
                    <span class="chip-val">{{ res.day_count_name }}</span>
                  </div>
                </div>
              }
            </div>
          </div>

          <!-- Section 3: Currency & Display Units -->
          <div class="settings-section">
            <h3 class="section-header">
              <svg class="section-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M4 10.781c.148 1.667 1.513 2.85 3.591 3.003V15h1.043v-1.216c2.27-.179 3.678-1.438 3.678-3.3 0-1.59-.947-2.51-2.956-3.028l-.722-.187V3.467c1.122.11 1.879.714 2.07 1.616h1.47c-.166-1.6-1.54-2.748-3.54-2.875V1H7.591v1.233c-1.939.23-3.27 1.472-3.27 3.156 0 1.454.966 2.483 2.661 2.917l.61.162v4.031c-1.195-.17-1.946-.82-2.113-1.718H4zm4.015-6.848v2.795c-.886-.237-1.408-.687-1.408-1.39 0-.82.648-1.314 1.408-1.405zm1.043 4.417c1.077.29 1.636.757 1.636 1.54 0 .913-.733 1.492-1.636 1.61V8.35z"/>
              </svg>
              <span>Reporting Currency &amp; Display Units</span>
            </h3>

            <div class="fields-stack">
              <div class="field-group">
                <label for="repCurrency">Reporting Currency</label>
                <select id="repCurrency" [(ngModel)]="req.report_currency" (change)="state.calculate()">
                  <option value="EUR">EUR (€) — Euro</option>
                  <option value="USD">USD ($) — US Dollar</option>
                  <option value="GBP">GBP (£) — British Pound</option>
                  <option value="CAD">CAD ($) — Canadian Dollar</option>
                  <option value="CHF">CHF — Swiss Franc</option>
                </select>
              </div>

              <div class="field-group">
                <label for="dispUnits">Report Display Scaling</label>
                <select id="dispUnits" [(ngModel)]="req.display_units" (change)="state.calculate()">
                  <option value="actual">Actual Amounts (Single Units)</option>
                  <option value="thousands">Thousands (000s)</option>
                  <option value="millions">Millions (mm)</option>
                </select>
              </div>

              <!-- Secondary Currency Display Toggle -->
              <div class="sec-curr-card">
                <label class="checkbox-container">
                  <input 
                    type="checkbox" 
                    [(ngModel)]="req.show_secondary_currency" 
                    (change)="state.calculate()" />
                  <span class="checkbox-text">Enable Secondary Currency Presentation</span>
                </label>

                @if (req.show_secondary_currency) {
                  <div class="sec-curr-inputs">
                    <div class="field-group">
                      <label for="secCurrency">Target Currency</label>
                      <select id="secCurrency" [(ngModel)]="req.secondary_currency" (change)="state.calculate()">
                        <option value="USD">USD ($)</option>
                        <option value="EUR">EUR (€)</option>
                        <option value="GBP">GBP (£)</option>
                        <option value="CHF">CHF</option>
                        <option value="CAD">CAD ($)</option>
                      </select>
                    </div>

                    <div class="field-group">
                      <label for="secFx">FX Rate (per 1.00 {{ req.report_currency }})</label>
                      <div class="input-wrapper">
                        <input 
                          id="secFx"
                          type="number" 
                          step="0.0001" 
                          [(ngModel)]="req.secondary_fx_rate" 
                          (change)="state.calculate()" 
                          placeholder="1.0000" />
                      </div>
                    </div>
                  </div>
                }
              </div>
            </div>
          </div>

          <!-- Section 4: Firm Identity & Logo Upload -->
          <div class="settings-section">
            <h3 class="section-header">
              <svg class="section-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M6.002 5.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z"/>
                <path d="M2.002 1a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V3a2 2 0 0 0-2-2h-12zm12 1a1 1 0 0 1 1 1v6.5l-3.777-1.947a.5.5 0 0 0-.577.093l-3.71 3.71-2.66-1.772a.5.5 0 0 0-.63.062L1.002 12V3a1 1 0 0 1 1-1h12z"/>
              </svg>
              <span>Firm Branding &amp; Logo</span>
            </h3>

            <div class="logo-section-content">
              @if (req.firm_logo_base64) {
                <div class="logo-preview-box">
                  <div class="preview-img-container">
                    <img [src]="req.firm_logo_base64" alt="Firm Logo Preview" class="logo-img" />
                  </div>
                  <div class="preview-actions">
                    <span class="preview-label">Firm Logo Active</span>
                    <button 
                      type="button" 
                      class="btn-danger-link" 
                      (click)="state.removeFirmLogo()"
                      aria-label="Remove firm logo">
                      ✕ Remove Logo
                    </button>
                  </div>
                </div>
              } @else {
                <div class="logo-upload-dropzone">
                  <input 
                    type="file" 
                    id="logoUploadInput" 
                    accept="image/png,image/jpeg,image/svg+xml" 
                    (change)="onLogoSelected($event)" 
                    style="display: none;" />
                  <label for="logoUploadInput" class="btn secondary upload-label">
                    <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                      <path d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5z"/>
                      <path d="M7.646 1.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 2.707V11.5a.5.5 0 0 1-1 0V2.707L5.354 4.854a.5.5 0 1 1-.708-.708l3-3z"/>
                    </svg>
                    <span>Upload Firm Logo (PNG / SVG)</span>
                  </label>
                  <p class="field-hint" style="margin-top: 8px;">
                    Embedded dynamically into executive report exhibits, cover sheets, and A4 PDF books.
                  </p>
                </div>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .settings-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
      gap: 20px;
    }

    .settings-section {
      background: #fbfdfc;
      border: 1px solid #dce6e4;
      border-radius: 10px;
      padding: 16px 18px;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e5efed;
      color: var(--teal);
      font-size: 13.5px;
    }

    .section-icon {
      width: 15px;
      height: 15px;
      color: var(--teal);
      flex-shrink: 0;
    }

    .fields-stack {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .term-chips-container {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-top: 6px;
      padding-top: 10px;
      border-top: 1px dashed #d8e5e2;
    }

    .term-chip {
      background: #eef5f3;
      border: 1px solid #c9ded9;
      border-radius: 6px;
      padding: 4px 8px;
      font-size: 11px;
      display: flex;
      gap: 5px;
    }

    .chip-label {
      color: #55676e;
      font-weight: 500;
    }

    .chip-val {
      color: var(--teal);
      font-weight: 700;
      font-variant-numeric: tabular-nums lining-nums;
    }

    .sec-curr-card {
      background: #f3f7f6;
      border: 1px solid #d4e2df;
      border-radius: 8px;
      padding: 12px 14px;
      margin-top: 4px;
    }

    .checkbox-container {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      user-select: none;
    }

    .checkbox-container input[type="checkbox"] {
      width: auto;
      margin: 0;
      cursor: pointer;
    }

    .checkbox-text {
      font-size: 12px;
      font-weight: 600;
      color: #24353a;
    }

    .sec-curr-inputs {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 12px;
      padding-top: 10px;
      border-top: 1px solid #d8e6e3;
    }

    .logo-section-content {
      padding: 8px 0;
    }

    .logo-preview-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      padding: 12px 16px;
      background: #ffffff;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
    }

    .preview-img-container {
      max-height: 48px;
      display: flex;
      align-items: center;
    }

    .logo-img {
      max-height: 44px;
      max-width: 180px;
      object-fit: contain;
    }

    .preview-actions {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;
    }

    .preview-label {
      font-size: 11px;
      color: #0d6e59;
      font-weight: 700;
    }

    .btn-danger-link {
      background: transparent;
      border: 0;
      color: var(--danger);
      font-size: 11.5px;
      font-weight: 600;
      cursor: pointer;
      padding: 2px 4px;
    }

    .btn-danger-link:hover {
      text-decoration: underline;
    }

    .logo-upload-dropzone {
      padding: 20px;
      border: 2px dashed #cbdad6;
      border-radius: 8px;
      text-align: center;
      background: #ffffff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }

    .upload-label {
      cursor: pointer;
    }

    .btn-icon {
      width: 13px;
      height: 13px;
      margin-right: 4px;
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
