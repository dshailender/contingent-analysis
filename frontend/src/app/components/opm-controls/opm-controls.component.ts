import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValuationStateService } from '../../services/valuation-state.service';

@Component({
  selector: 'app-opm-controls',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state.request(); as req) {
      <div class="card accent-card">
        <h2>OPM Calibration & Valuation Parameters</h2>
        <p class="section-desc">
          Configure Black-Scholes volatility assumptions, risk-free interest rates, subject adjustments, and backsolve calibration inputs.
        </p>

        <div class="opm-sections">
          <!-- Backsolve Calibration Section -->
          <div class="param-panel">
            <div class="panel-header">
              <span class="panel-icon">🎯</span>
              <h3>Backsolve Calibration ({{ req.calibration_date }})</h3>
            </div>

            <div class="panel-grid">
              <div class="field-group">
                <label>Calibration Security Class <span class="req">*</span></label>
                <select [(ngModel)]="req.calibration_security_name" (change)="state.calculate()">
                  @for (name of state.availablePreferredSecurities(); track name) {
                    <option [value]="name">{{ name }}</option>
                  }
                </select>
                <span class="field-hint">Recent transacted financing class</span>
              </div>

              <div class="field-group">
                <label>Observed Transaction Price ({{ state.currencySymbol() }}/sh) <span class="req">*</span></label>
                <input 
                  type="number" 
                  step="0.01" 
                  [(ngModel)]="req.transaction_price" 
                  (change)="state.calculate()" 
                  [class.invalid]="req.transaction_price <= 0" />
                <span class="field-hint">Clean issue price per share</span>
              </div>

              <div class="field-group">
                <label>Calibration Risk-Free Rate (%)</label>
                <input 
                  type="number" 
                  step="0.0001" 
                  [(ngModel)]="req.rf_calibration" 
                  (change)="state.calculate()" />
                <span class="field-hint">
                  Annual effective
                  @if (state.response(); as res) {
                    · rc = {{ res.rf_calibration_continuous | number:'1.4-4' }}% continuous
                  }
                </span>
              </div>

              <div class="field-group">
                <label>Calibration Volatility (%) <span class="req">*</span></label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.vol_calibration" 
                  (change)="state.calculate()" 
                  [class.invalid]="req.vol_calibration <= 0" />
                <span class="field-hint">Annualized asset/equity volatility</span>
              </div>

              <div class="field-group">
                <label>Calibration Dividend Yield (%)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.dividend_yield_calibration" 
                  (change)="state.calculate()" />
                <span class="field-hint">Expected continuous yield</span>
              </div>
            </div>

            @if (state.response(); as res) {
              <div class="solved-banner">
                <span class="banner-label">Calibration Solved Equity Value:</span>
                <span class="banner-val">{{ state.currencySymbol() }}{{ res.calibration_solved_equity | number:'1.2-2' }}</span>
              </div>
            }
          </div>

          <!-- Valuation Date Section -->
          <div class="param-panel">
            <div class="panel-header" style="display: flex; justify-content: space-between; align-items: center;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span class="panel-icon">📈</span>
                <h3>Valuation Date Allocation ({{ req.valuation_date }})</h3>
              </div>
              <button type="button" class="btn-refresh-curve" (click)="state.refreshRiskFreeCurves()" title="Refresh official risk-free yield curve observations">
                ↻ Refresh Curve
              </button>
            </div>

            <div class="panel-grid">
              <div class="field-group">
                <label>Valuation Risk-Free Rate (%)</label>
                <input 
                  type="number" 
                  step="0.0001" 
                  [(ngModel)]="req.rf_valuation" 
                  (change)="state.calculate()" />
                <span class="field-hint">
                  Interpolated to {{ req.exit_date }}
                  @if (state.response(); as res) {
                    · rc = {{ res.rf_valuation_continuous | number:'1.4-4' }}% continuous
                  }
                </span>
              </div>

              <div class="field-group">
                <label>Valuation Volatility (%) <span class="req">*</span></label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.vol_valuation" 
                  (change)="state.calculate()" 
                  [class.invalid]="req.vol_valuation <= 0" />
                <span class="field-hint">Annualized volatility as of valuation date</span>
              </div>

              <div class="field-group">
                <label>Valuation Dividend Yield (%)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.dividend_yield_valuation" 
                  (change)="state.calculate()" />
                <span class="field-hint">Continuous dividend yield</span>
              </div>

              <div class="field-group">
                <label>Market Adjustment (%)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.market_adjustment" 
                  (change)="state.calculate()" />
                <span class="field-hint">Public peer index return</span>
              </div>

              <div class="field-group">
                <label>Company Adjustment (%)</label>
                <input 
                  type="number" 
                  step="0.1" 
                  [(ngModel)]="req.company_adjustment" 
                  (change)="state.calculate()" />
                <span class="field-hint">Subject performance vs budget</span>
              </div>
            </div>

            @if (state.response(); as res) {
              <div class="concluded-banner">
                <span class="banner-label">Concluded Total Enterprise Equity:</span>
                <span class="banner-val">{{ state.currencySymbol() }}{{ res.concluded_equity_value | number:'1.2-2' }}</span>
              </div>
            }
          </div>
        </div>

        <!-- Comparative Waterfall Settings -->
        <div class="waterfall-source-card">
          <h3>Comparative Waterfall Scenario Equity Source</h3>
          <div class="radio-row">
            <label class="radio-label">
              <input 
                type="radio" 
                name="wfSource" 
                value="concluded" 
                [(ngModel)]="req.waterfall_equity_source" 
                (change)="state.calculate()" />
              <span>Use Concluded Equity Value ({{ state.currencySymbol() }}{{ (state.response()?.concluded_equity_value || 0) | number:'1.2-2' }})</span>
            </label>

            <label class="radio-label">
              <input 
                type="radio" 
                name="wfSource" 
                value="manual" 
                [(ngModel)]="req.waterfall_equity_source" 
                (change)="state.calculate()" />
              <span>Manual Scenario Equity Value</span>
            </label>
          </div>

          @if (req.waterfall_equity_source === 'manual') {
            <div class="manual-wf-input">
              <label>Manual Scenario Exit Equity ({{ state.currencySymbol() }}):</label>
              <input 
                type="number" 
                step="10000" 
                [(ngModel)]="req.manual_waterfall_equity" 
                (change)="state.calculate()" />
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .section-desc {
      color: #687386;
      font-size: 13px;
      margin-bottom: 18px;
    }

    .opm-sections {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
    }

    .param-panel {
      background: #fbfdfc;
      border: 1px solid #dce6e4;
      border-radius: 10px;
      padding: 16px;
    }

    .panel-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e7efed;
    }

    .btn-refresh-curve {
      padding: 4px 10px;
      font-size: 11px;
      font-weight: 600;
      color: #0f5f91;
      background: #eef5fa;
      border: 1px solid #c9dff0;
      border-radius: 4px;
      cursor: pointer;
      transition: background 0.15s;
    }

    .btn-refresh-curve:hover {
      background: #dbeafe;
    }


    .panel-icon {
      font-size: 16px;
    }

    .panel-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 12px;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .field-group label {
      font-size: 11.5px;
      font-weight: 600;
      color: var(--slate);
    }

    .field-hint {
      font-size: 10px;
      color: #7a8693;
    }

    .req {
      color: var(--danger);
    }

    .solved-banner, .concluded-banner {
      margin-top: 14px;
      padding: 10px 12px;
      border-radius: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .solved-banner {
      background: #eef5f3;
      border: 1px solid #d3e5e1;
    }

    .concluded-banner {
      background: #f9f6ee;
      border: 1px solid #e9dec3;
    }

    .banner-label {
      font-size: 12px;
      font-weight: 600;
      color: #3f5056;
    }

    .banner-val {
      font-size: 15px;
      font-weight: 800;
      color: var(--teal);
    }

    .waterfall-source-card {
      background: #f7faf9;
      border: 1px solid #dce5e3;
      border-radius: 10px;
      padding: 14px 16px;
    }

    .radio-row {
      display: flex;
      gap: 24px;
      margin: 10px 0;
      flex-wrap: wrap;
    }

    .radio-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12.5px;
      cursor: pointer;
    }

    .manual-wf-input {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-top: 10px;
    }

    .manual-wf-input label {
      font-size: 12px;
      font-weight: 600;
    }
  `]
})
export class OpmControlsComponent {
  readonly state = inject(ValuationStateService);
}

