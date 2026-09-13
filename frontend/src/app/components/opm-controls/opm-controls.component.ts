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
        <h2>OPM Calibration &amp; Valuation Assumptions</h2>
        <p class="section-desc">
          Configure Black-Scholes asset volatility, risk-free interest rates, market index adjustments, and backsolve calibration targets.
        </p>

        <div class="opm-sections-grid">
          <!-- Panel 1: Calibration Date Backsolve -->
          <div class="param-panel">
            <div class="panel-header">
              <svg class="panel-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                <path d="M8 13A5 5 0 1 1 8 3a5 5 0 0 1 0 10zm0 1A6 6 0 1 0 8 2a6 6 0 0 0 0 12z"/>
                <path d="M8 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm0 1a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"/>
                <path d="M9.5 8a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z"/>
              </svg>
              <h3>Backsolve Calibration ({{ req.calibration_date }})</h3>
            </div>

            <div class="panel-grid">
              <div class="field-group">
                <label for="calSec">Calibration Target Class <span class="req">*</span></label>
                <select id="calSec" [(ngModel)]="req.calibration_security_name" (change)="state.calculate()">
                  @for (name of state.availableCalibrationSecurities(); track name) {
                    <option [value]="name">{{ name }}</option>
                  }
                </select>
                <span class="field-hint">Recent priced financing round class</span>
              </div>

              <div class="field-group">
                <label for="transPrice">Observed Transaction Price <span class="req">*</span></label>
                <div class="input-wrapper">
                  <span class="input-prefix">{{ state.currencySymbol() }}</span>
                  <input 
                    id="transPrice"
                    type="number" 
                    step="0.01" 
                    [(ngModel)]="req.transaction_price" 
                    (change)="state.calculate()" 
                    class="has-prefix"
                    placeholder="2021.90"
                    [class.invalid]="req.transaction_price <= 0" />
                </div>
                <span class="field-hint">Clean issue price per share</span>
              </div>

              <div class="field-group">
                <label for="rfCal">Risk-Free Rate — Annual Effective</label>
                <div class="input-wrapper">
                  <input 
                    id="rfCal"
                    type="number" 
                    step="0.0001" 
                    [(ngModel)]="req.rf_calibration" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="2.4500" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">
                  @if (state.response(); as res) {
                    Continuous rate rc = {{ res.rf_calibration_continuous | number:'1.4-4' }}%
                  } @else {
                    Annual effective yield
                  }
                </span>
              </div>

              <div class="field-group">
                <label for="volCal">Calibration Volatility <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input 
                    id="volCal"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.vol_calibration" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="48.5"
                    [class.invalid]="req.vol_calibration <= 0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Annualized asset/equity volatility</span>
              </div>

              <div class="field-group">
                <label for="divCal">Calibration Dividend Yield</label>
                <div class="input-wrapper">
                  <input 
                    id="divCal"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.dividend_yield_calibration" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="0.0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Expected continuous dividend yield</span>
              </div>
            </div>

            @if (state.response(); as res) {
              <div class="solved-banner">
                <div class="banner-inner">
                  <span class="banner-label">Calibration Solved Enterprise Equity Value:</span>
                  <span class="banner-val tabular">{{ state.currencySymbol() }}{{ res.calibration_solved_equity | number:'1.2-2' }}</span>
                </div>
                <span class="banner-sub">Reconciles target share price to {{ state.currencySymbol() }}{{ req.transaction_price | number:'1.2-2' }}</span>
              </div>
            }
          </div>

          <!-- Panel 2: Valuation Date Allocation -->
          <div class="param-panel">
            <div class="panel-header" style="justify-content: space-between;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <svg class="panel-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M0 0h1v15h15v1H0V0Zm14.817 3.113a.5.5 0 0 1 .07.704l-4.5 5.5a.5.5 0 0 1-.74.037L7.06 6.767l-3.656 5.027a.5.5 0 0 1-.808-.588l4-5.5a.5.5 0 0 1 .758-.06l2.609 2.61 4.15-5.073a.5.5 0 0 1 .704-.07Z"/>
                </svg>
                <h3>Valuation Date Allocation ({{ req.valuation_date }})</h3>
              </div>
              <button 
                type="button" 
                class="btn-refresh-curve" 
                (click)="state.refreshRiskFreeCurves()" 
                title="Refresh benchmark sovereign yield curves from official sources"
                aria-label="Refresh official risk-free yield curve">
                <svg class="refresh-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                  <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
                </svg>
                <span>Refresh Curve</span>
              </button>
            </div>

            <div class="panel-grid">
              <div class="field-group">
                <label for="rfVal">Risk-Free Rate — Annual Effective</label>
                <div class="input-wrapper">
                  <input 
                    id="rfVal"
                    type="number" 
                    step="0.0001" 
                    [(ngModel)]="req.rf_valuation" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="2.5600" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">
                  @if (state.response(); as res) {
                    Continuous rate rc = {{ res.rf_valuation_continuous | number:'1.4-4' }}%
                  } @else {
                    Interpolated to {{ req.exit_date }}
                  }
                </span>
              </div>

              <div class="field-group">
                <label for="volVal">Valuation Volatility <span class="req">*</span></label>
                <div class="input-wrapper">
                  <input 
                    id="volVal"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.vol_valuation" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="48.5"
                    [class.invalid]="req.vol_valuation <= 0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Asset volatility as-of valuation date</span>
              </div>

              <div class="field-group">
                <label for="divVal">Valuation Dividend Yield</label>
                <div class="input-wrapper">
                  <input 
                    id="divVal"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.dividend_yield_valuation" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="0.0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Continuous dividend yield</span>
              </div>

              <div class="field-group">
                <label for="mktAdj">Market Adjustment</label>
                <div class="input-wrapper">
                  <input 
                    id="mktAdj"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.market_adjustment" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="0.0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Guideline public peer index return</span>
              </div>

              <div class="field-group">
                <label for="compAdj">Company Adjustment</label>
                <div class="input-wrapper">
                  <input 
                    id="compAdj"
                    type="number" 
                    step="0.1" 
                    [(ngModel)]="req.company_adjustment" 
                    (change)="state.calculate()" 
                    class="has-suffix"
                    placeholder="0.0" />
                  <span class="input-suffix">%</span>
                </div>
                <span class="field-hint">Subject performance adjustment</span>
              </div>
            </div>

            @if (state.response(); as res) {
              <div class="concluded-banner">
                <div class="banner-inner">
                  <span class="banner-label">Concluded Total Enterprise Equity:</span>
                  <span class="banner-val tabular">{{ state.currencySymbol() }}{{ res.concluded_equity_value | number:'1.2-2' }}</span>
                </div>
                <span class="banner-sub">Term: {{ res.term_valuation | number:'1.2-2' }} yrs • Vol: {{ res.valuation_opm.volatility * 100 | number:'1.1-1' }}% • Rf: {{ res.rf_valuation_effective | number:'1.2-2' }}%</span>
              </div>
            }
          </div>
        </div>

        <!-- Comparative Waterfall Scenario Equity Setting -->
        <div class="waterfall-source-card">
          <h3>Comparative Waterfall Scenario Equity Source</h3>
          <p class="section-desc" style="margin-bottom: 10px;">
            Specify the total exit proceeds applied to contractual liquidation preference waterfalls and side-by-side comparative exhibits.
          </p>

          <div class="radio-row">
            <label class="radio-label">
              <input 
                type="radio" 
                name="wfSource" 
                value="concluded" 
                [(ngModel)]="req.waterfall_equity_source" 
                (change)="state.calculate()" />
              <span>Use Concluded Equity Value (<strong>{{ state.currencySymbol() }}{{ (state.response()?.concluded_equity_value || 0) | number:'1.2-2' }}</strong>)</span>
            </label>

            <label class="radio-label">
              <input 
                type="radio" 
                name="wfSource" 
                value="manual" 
                [(ngModel)]="req.waterfall_equity_source" 
                (change)="state.calculate()" />
              <span>Manual Scenario Exit Value</span>
            </label>
          </div>

          @if (req.waterfall_equity_source === 'manual') {
            <div class="manual-wf-input-wrap">
              <div class="field-group" style="max-width: 320px;">
                <label for="manEquity">Manual Scenario Exit Equity ({{ state.currencySymbol() }})</label>
                <div class="input-wrapper">
                  <span class="input-prefix">{{ state.currencySymbol() }}</span>
                  <input 
                    id="manEquity"
                    type="number" 
                    step="50000" 
                    [(ngModel)]="req.manual_waterfall_equity" 
                    (change)="state.calculate()" 
                    class="has-prefix"
                    placeholder="75000000" />
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .opm-sections-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
    }

    .param-panel {
      background: #fbfdfc;
      border: 1px solid #dce6e4;
      border-radius: 10px;
      padding: 16px 18px;
    }

    .panel-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e7efed;
    }

    .panel-icon {
      width: 15px;
      height: 15px;
      color: var(--teal);
      flex-shrink: 0;
    }

    .btn-refresh-curve {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 4px 9px;
      font-size: 11px;
      font-weight: 600;
      color: #08615e;
      background: #eef5f3;
      border: 1px solid #c9dfdb;
      border-radius: 5px;
      cursor: pointer;
      transition: background 0.12s;
    }

    .btn-refresh-curve:hover {
      background: #dbeefb;
    }

    .refresh-icon {
      width: 11px;
      height: 11px;
    }

    .panel-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }

    .solved-banner, .concluded-banner {
      padding: 12px 14px;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      gap: 3px;
    }

    .solved-banner {
      background: linear-gradient(135deg, #f0f7f6 0%, #e6f3f0 100%);
      border: 1px solid #c5dfd8;
    }

    .concluded-banner {
      background: linear-gradient(135deg, #fbf7ee 0%, #f7efdc 100%);
      border: 1px solid #e2d2b4;
    }

    .banner-inner {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 10px;
      flex-wrap: wrap;
    }

    .banner-label {
      font-size: 11px;
      font-weight: 600;
      color: #3e5055;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .banner-val {
      font-size: 18px;
      font-weight: 800;
      color: #123035;
    }

    .concluded-banner .banner-val {
      color: #8c5b16;
    }

    .banner-sub {
      font-size: 10.5px;
      color: #6a7c81;
    }

    .waterfall-source-card {
      background: #f8fbfa;
      border: 1px solid #dce7e5;
      border-radius: 10px;
      padding: 16px 18px;
    }

    .radio-row {
      display: flex;
      gap: 24px;
      margin: 12px 0;
      flex-wrap: wrap;
    }

    .radio-label {
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 12.5px;
      cursor: pointer;
      color: #27393d;
    }

    .radio-label input[type="radio"] {
      width: auto;
      margin: 0;
      cursor: pointer;
    }

    .manual-wf-input-wrap {
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid #e1ebe9;
    }
  `]
})
export class OpmControlsComponent {
  readonly state = inject(ValuationStateService);
}
