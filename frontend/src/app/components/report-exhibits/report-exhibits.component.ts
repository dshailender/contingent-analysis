import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValuationStateService } from '../../services/valuation-state.service';

@Component({
  selector: 'app-report-exhibits',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (state.response(); as res) {
      <div class="exhibits-wrapper">
        <!-- Top Executive KPI Summary Cards -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <span class="kpi-label">Calibration Solved Equity</span>
            <span class="kpi-val">{{ state.currencySymbol() }}{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">As of {{ res.calibration_date }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Valuation Concluded Equity</span>
            <span class="kpi-val">{{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">As of {{ res.valuation_date }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Client Portfolio Fair Value</span>
            <span class="kpi-val">{{ state.currencySymbol() }}{{ (res.holdings.total_value * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">Total invested: {{ state.currencySymbol() }}{{ (res.holdings.total_cost * res.display_scale) | number:'1.2-2' }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Consolidated Portfolio MOIC</span>
            <span class="kpi-val">{{ (res.holdings.consolidated_moic || 0) | number:'1.2-2' }}x</span>
            <span class="kpi-sub">Multiple on Invested Capital</span>
          </div>
        </div>

        <!-- Exhibit Navigation Pills -->
        <div class="exhibit-pill-nav">
          <button [class.active]="selectedExhibit() === 'Cover'" (click)="selectedExhibit.set('Cover')">Cover Sheet</button>
          <button [class.active]="selectedExhibit() === 'Index'" (click)="selectedExhibit.set('Index')">Index & Summary</button>
          <button [class.active]="selectedExhibit() === '1.0'" (click)="selectedExhibit.set('1.0')">Exhibit 1.0: Holdings</button>
          <button [class.active]="selectedExhibit() === '2.0'" (click)="selectedExhibit.set('2.0')">Exhibit 2.0: Cal Cap Table</button>
          <button [class.active]="selectedExhibit() === '3.0'" (click)="selectedExhibit.set('3.0')">Exhibit 3.0: Cal Breakpoints</button>
          <button [class.active]="selectedExhibit() === '4.0'" (click)="selectedExhibit.set('4.0')">Exhibit 4.0: Cal OPM Backsolve</button>
          <button [class.active]="selectedExhibit() === '5.0'" (click)="selectedExhibit.set('5.0')">Exhibit 5.0: Cal Risk-Free</button>
          <button [class.active]="selectedExhibit() === '6.0'" (click)="selectedExhibit.set('6.0')">Exhibit 6.0: Val Cap Table</button>
          <button [class.active]="selectedExhibit() === '7.0'" (click)="selectedExhibit.set('7.0')">Exhibit 7.0: Val Breakpoints</button>
          <button [class.active]="selectedExhibit() === '8.0'" (click)="selectedExhibit.set('8.0')">Exhibit 8.0: Val OPM Allocation</button>
          <button [class.active]="selectedExhibit() === '9.0'" (click)="selectedExhibit.set('9.0')">Exhibit 9.0: Val Risk-Free</button>
          <button [class.active]="selectedExhibit() === '10.0'" (click)="selectedExhibit.set('10.0')">Exhibit 10.0: Comparative Waterfall</button>
        </div>

        <!-- ========================================================================= -->
        <!-- COVER SHEET -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === 'Cover') {
          <div class="card accent-card cover-sheet-view">
            @if (res.firm_logo_base64) {
              <div class="cover-logo-wrap">
                <img [src]="res.firm_logo_base64" alt="Firm Logo" class="cover-logo-img" />
              </div>
            } @else {
              <div class="cover-eyebrow">CONTINGENT CLAIMS VALUATION ENGINE</div>
            }

            <h1 class="cover-main-title">CONTINGENT CLAIMS VALUATION REPORT</h1>
            <p class="cover-sub-title">Option Pricing Method (OPM) Backsolve &amp; Allocation Analysis prepared for {{ res.client_name }}</p>

            <div class="cover-grid">
              <div class="cover-cell">
                <span class="cell-label">Subject Company</span>
                <span class="cell-value">{{ res.company_name }}</span>
              </div>
              <div class="cover-cell">
                <span class="cell-label">Report Purpose</span>
                <span class="cell-value">{{ res.report_purpose }}</span>
              </div>
              <div class="cover-cell">
                <span class="cell-label">Report Status</span>
                <span class="cell-value">{{ res.report_status }}</span>
              </div>
              <div class="cover-cell">
                <span class="cell-label">Calibration Date</span>
                <span class="cell-value">{{ res.calibration_date }}</span>
              </div>
              <div class="cover-cell">
                <span class="cell-label">Valuation Date</span>
                <span class="cell-value">{{ res.valuation_date }}</span>
              </div>
              <div class="cover-cell">
                <span class="cell-label">Concluded Total Equity</span>
                <span class="cell-value highlight">{{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</span>
              </div>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- INDEX OF EXHIBITS -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === 'Index') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Index of Exhibits &amp; Key Valuation Assumptions</h2>
              <span class="unit-badge">{{ res.company_name }} • As of {{ res.valuation_date }}</span>
            </div>

            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th style="width: 15%;">Exhibit</th>
                    <th style="text-align: left; width: 60%;">Description</th>
                    <th style="width: 25%;">Scope / Effective Date</th>
                  </tr>
                </thead>
                <tbody>
                  <tr><td><strong>Exhibit 1.0</strong></td><td>Client Holdings Summary &amp; Concluded Portfolio Value</td><td>Valuation Date ({{ res.valuation_date }})</td></tr>
                  <tr><td><strong>Exhibit 2.0</strong></td><td>Capitalization Structure &amp; Security Terms</td><td>Calibration Date ({{ res.calibration_date }})</td></tr>
                  <tr><td><strong>Exhibit 3.0</strong></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Calibration Date ({{ res.calibration_date }})</td></tr>
                  <tr><td><strong>Exhibit 4.0</strong></td><td>OPM Backsolve Mechanics &amp; Tranche Option Pricing</td><td>Calibration Date ({{ res.calibration_date }})</td></tr>
                  <tr><td><strong>Exhibit 5.0</strong></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Calibration Date ({{ res.calibration_date }})</td></tr>
                  <tr><td><strong>Exhibit 6.0</strong></td><td>Capitalization Structure &amp; Security Terms</td><td>Valuation Date ({{ res.valuation_date }})</td></tr>
                  <tr><td><strong>Exhibit 7.0</strong></td><td>Breakpoint Schedule &amp; Claims Allocation Matrix</td><td>Valuation Date ({{ res.valuation_date }})</td></tr>
                  <tr><td><strong>Exhibit 8.0</strong></td><td>OPM Value Allocation Matrix &amp; Concluded Fair Values</td><td>Valuation Date ({{ res.valuation_date }})</td></tr>
                  <tr><td><strong>Exhibit 9.0</strong></td><td>Risk-Free Rate Curve &amp; Interpolation Analysis</td><td>Valuation Date ({{ res.valuation_date }})</td></tr>
                  <tr><td><strong>Exhibit 10.0</strong></td><td>Comparative Liquidation Waterfall Schedule</td><td>Side-by-Side (Calibration vs. Valuation)</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 1.0: CLIENT HOLDINGS SUMMARY -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '1.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 1.0: Client Portfolio Holdings &amp; Fair Value Summary</h2>
              <span class="unit-badge">Amounts in {{ res.display_units }} ({{ res.report_currency }})</span>
            </div>

            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Fund / Vehicle</th>
                    <th>Security Class</th>
                    <th style="text-align: right;">Units Held</th>
                    <th style="text-align: right;">Invested Cost</th>
                    <th style="text-align: right;">Fair Value / Sh</th>
                    <th style="text-align: right;">Concluded Value</th>
                    @if (res.show_secondary_currency) {
                      <th style="text-align: right;">Value ({{ res.secondary_currency }})</th>
                    }
                    <th style="text-align: right;">Class Own %</th>
                    <th style="text-align: right;">FD Own %</th>
                    <th style="text-align: right;">MOIC</th>
                  </tr>
                </thead>
                <tbody>
                  @for (h of res.holdings.items; track $index) {
                    <tr>
                      <td><strong>{{ h.fund }}</strong></td>
                      <td>{{ h.security }}</td>
                      <td style="text-align: right;">{{ h.units | number:'1.0-0' }}</td>
                      <td style="text-align: right;">{{ (h.cost * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ h.fair_value_per_share | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ (h.concluded_fair_value * res.display_scale) | number:'1.2-2' }}</td>
                      @if (res.show_secondary_currency) {
                        <td style="text-align: right;">{{ (h.concluded_fair_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                      }
                      <td style="text-align: right;">{{ (h.class_ownership_pct * 100) | number:'1.2-2' }}%</td>
                      <td style="text-align: right;">{{ (h.fully_diluted_ownership_pct * 100) | number:'1.2-2' }}%</td>
                      <td style="text-align: right;"><strong>{{ (h.moic || 0) | number:'1.2-2' }}x</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="3"><strong>Total Client Portfolio</strong></td>
                    <td style="text-align: right;">{{ (res.holdings.total_cost * res.display_scale) | number:'1.2-2' }}</td>
                    <td></td>
                    <td style="text-align: right;">{{ (res.holdings.total_value * res.display_scale) | number:'1.2-2' }}</td>
                    @if (res.show_secondary_currency) {
                      <td style="text-align: right;">{{ (res.holdings.total_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                    }
                    <td></td>
                    <td></td>
                    <td style="text-align: right;"><strong>{{ (res.holdings.consolidated_moic || 0) | number:'1.2-2' }}x</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 2.0: CALIBRATION CAP TABLE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '2.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 2.0: Capitalization Table &amp; Security Terms — Calibration Date</h2>
              <span class="unit-badge">Effective Date: {{ res.calibration_date }}</span>
            </div>

            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Security</th>
                    <th>Subtype</th>
                    <th style="text-align: right;">Shares</th>
                    <th style="text-align: right;">Orig. Issue Price</th>
                    <th style="text-align: right;">Liq. Mult</th>
                    <th style="text-align: right;">Seniority</th>
                    <th style="text-align: right;">Participation</th>
                    <th style="text-align: right;">Pref / Share</th>
                    <th style="text-align: right;">Total Liq. Pref.</th>
                    <th style="text-align: right;">FD Shares</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of res.calibration_derived_securities; track s.security) {
                    <tr>
                      <td><strong>{{ s.security }}</strong></td>
                      <td>{{ s.security_subtype }}</td>
                      <td style="text-align: right;">{{ s.shares | number:'1.0-0' }}</td>
                      <td style="text-align: right;">{{ s.original_issue_price > 0 ? (s.original_issue_price | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.liquidation_multiplier | number:'1.2-2' }}x</td>
                      <td style="text-align: right;">{{ s.seniority < 999 ? s.seniority : '—' }}</td>
                      <td style="text-align: right;">{{ s.participation }}</td>
                      <td style="text-align: right;">{{ s.liquidation_preference_per_share > 0 ? (s.liquidation_preference_per_share | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.total_liquidation_preference > 0 ? ((s.total_liquidation_preference * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.fully_diluted_shares | number:'1.0-0' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 3.0: CALIBRATION BREAKPOINTS & CLAIMS MATRIX -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '3.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 3.0: Breakpoint Schedule &amp; Claims Matrix — Calibration Date</h2>
              <span class="unit-badge">As of {{ res.calibration_date }}</span>
            </div>

            <h3 class="sub-table-header">Breakpoint Equity Thresholds</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th style="width: 8%;">Tier</th>
                    <th style="width: 18%; text-align: right;">Start Equity Value</th>
                    <th style="width: 18%; text-align: right;">End Equity Value</th>
                    <th style="width: 18%; text-align: right;">Tranche Width</th>
                    <th style="width: 38%;">Economic Trigger Event / Claimant Classes</th>
                  </tr>
                </thead>
                <tbody>
                  @for (bp of res.calibration_breakpoints; track bp.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ bp.tier }}</span></td>
                      <td style="text-align: right;">{{ (bp.start_equity * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ bp.is_thereafter ? 'Thereafter' : ((bp.end_equity * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;">{{ bp.is_thereafter ? '—' : (( (bp.width || (bp.end_equity - bp.start_equity)) * res.display_scale) | number:'1.2-2') }}</td>
                      <td>{{ bp.event_description || bp.claimants_description || bp.events_description }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Claims Sharing Percentage Matrix by Breakpoint Tier</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th style="width: 8%;">Tier</th>
                    <th style="width: 22%;">Equity Interval</th>
                    @for (sec of res.calibration_derived_securities; track sec.security) {
                      <th style="text-align: right;">{{ sec.security }}</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (cl of res.calibration_claims; track cl.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ cl.tier }}</span></td>
                      <td>
                        {{ (cl.from_equity !== undefined ? cl.from_equity : cl.start_equity || 0) * res.display_scale | number:'1.2-2' }} –
                        {{ cl.is_thereafter ? 'Thereafter' : (((cl.to_equity !== undefined ? cl.to_equity : cl.end_equity || 0) * res.display_scale) | number:'1.2-2') }}
                      </td>
                      @for (sec of res.calibration_derived_securities; track sec.security) {
                        <td style="text-align: right;">
                          {{ (cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) > 0 ? (((cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) * 100) | number:'1.1-1') + '%' : '—' }}
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 4.0: CALIBRATION OPM BACKSOLVE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '4.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 4.0: OPM Backsolve Mechanics &amp; Value Allocation — Calibration Date</h2>
              <span class="unit-badge">Term: {{ res.term_calibration | number:'1.2-2' }}y • Vol: {{ res.calibration_opm.volatility * 100 | number:'1.1-1' }}% • Rf: {{ res.rf_calibration_effective | number:'1.2-2' }}%</span>
            </div>

            <h3 class="sub-table-header">Black-Scholes Call Option Tranche Pricing</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Tranche</th>
                    <th style="text-align: right;">Lower Strike (Xk-1)</th>
                    <th style="text-align: right;">Upper Strike (Xk)</th>
                    <th style="text-align: right;">Call C(Xk-1)</th>
                    <th style="text-align: right;">Call C(Xk)</th>
                    <th style="text-align: right;">Incremental Value (ΔCk)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (tr of res.calibration_opm.tranches; track tr.tier) {
                    <tr>
                      <td><span class="badge tier">Tranche {{ tr.tier }}</span></td>
                      <td style="text-align: right;">{{ (tr.strike_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ tr.strike_high > 1e12 ? '∞' : ((tr.strike_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;">{{ (tr.call_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ tr.strike_high > 1e12 ? '0.00' : ((tr.call_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;"><strong>{{ (tr.incremental_call * res.display_scale) | number:'1.2-2' }}</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="5"><strong>Total Solved Enterprise Equity Value</strong></td>
                    <td style="text-align: right;">{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Security-by-Security Allocated Values</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Security Class</th>
                    <th style="text-align: right;">Shares Out</th>
                    <th style="text-align: right;">Total Allocated Value</th>
                    <th style="text-align: right;">Allocated %</th>
                    <th style="text-align: right;">Per-Share Value</th>
                  </tr>
                </thead>
                <tbody>
                  @for (sec of res.calibration_derived_securities; track sec.security) {
                    <tr>
                      <td><strong>{{ sec.security }}</strong></td>
                      <td style="text-align: right;">{{ sec.shares | number:'1.0-0' }}</td>
                      <td style="text-align: right;">{{ ((res.calibration_opm.allocated_values[sec.security] || 0) * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ ((res.calibration_opm.percent_allocations[sec.security] || 0) * 100) | number:'1.2-2' }}%</td>
                      <td style="text-align: right;">
                        <strong>{{ (res.calibration_opm.per_share_values[sec.security] || 0) | number:'1.2-2' }}</strong>
                      </td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td><strong>Total Solved Enterprise Equity</strong></td>
                    <td></td>
                    <td style="text-align: right;">{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</td>
                    <td style="text-align: right;">100.00%</td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 5.0: CALIBRATION RISK-FREE RATE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '5.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 5.0: Risk-Free Rate Curve &amp; Interpolation — Calibration Date</h2>
              <span class="unit-badge">Term to Exit: {{ res.term_calibration | number:'1.2-2' }} Years</span>
            </div>

            @if (res.calibration_rf_analysis; as rf) {
              <div class="rf-summary-box">
                <p><strong>Curve Source:</strong> {{ rf.source_name }} (As-of {{ rf.as_of_date }})</p>
                <p><strong>Interpolated Annual Effective Rate:</strong> {{ rf.interpolated_annual_effective_rate * 100 | number:'1.4-4' }}%</p>
                <p><strong>Continuous Compounding Rate (rc):</strong> {{ rf.continuous_rate * 100 | number:'1.4-4' }}% (Formula: ln(1 + reff))</p>
                <p class="rf-meta"><em>{{ rf.interpolation_metadata }}</em></p>
              </div>

              @if (rf.curve_points && rf.curve_points.length > 0) {
                <h3 class="sub-table-header" style="margin-top: 18px;">Published Benchmark Yield Points</h3>
                <div class="spreadsheet-container">
                  <table class="spreadsheet-table">
                    <thead>
                      <tr>
                        <th>Tenor / Benchmark Maturity</th>
                        <th style="text-align: right;">Published Yield (%)</th>
                        <th style="text-align: right;">Maturity Term (Years)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (pt of rf.curve_points; track pt.tenor_name) {
                        <tr>
                          <td><strong>{{ pt.tenor_name }}</strong></td>
                          <td style="text-align: right;">{{ pt.rate_percent | number:'1.2-2' }}%</td>
                          <td style="text-align: right;">{{ pt.tenor_years | number:'1.2-2' }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            }
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 6.0: VALUATION CAP TABLE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '6.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 6.0: Capitalization Table &amp; Security Terms — Valuation Date</h2>
              <span class="unit-badge">Effective Date: {{ res.valuation_date }}</span>
            </div>

            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Security</th>
                    <th>Subtype</th>
                    <th style="text-align: right;">Shares</th>
                    <th style="text-align: right;">Orig. Issue Price</th>
                    <th style="text-align: right;">Conv. Price</th>
                    <th style="text-align: right;">Seniority</th>
                    <th style="text-align: right;">Participation</th>
                    <th style="text-align: right;">Total Liq. Pref.</th>
                    <th style="text-align: right;">Conv. Threshold</th>
                    <th style="text-align: right;">FD Shares</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of res.valuation_derived_securities; track s.security) {
                    <tr>
                      <td><strong>{{ s.security }}</strong></td>
                      <td>{{ s.security_subtype }}</td>
                      <td style="text-align: right;">{{ s.shares | number:'1.0-0' }}</td>
                      <td style="text-align: right;">{{ s.original_issue_price > 0 ? (s.original_issue_price | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.conversion_price > 0 ? (s.conversion_price | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.seniority < 999 ? s.seniority : '—' }}</td>
                      <td style="text-align: right;">{{ s.participation }}</td>
                      <td style="text-align: right;">{{ s.total_liquidation_preference > 0 ? ((s.total_liquidation_preference * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.conversion_threshold_equity > 0 ? ((s.conversion_threshold_equity * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ s.fully_diluted_shares | number:'1.0-0' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 7.0: VALUATION BREAKPOINTS & CLAIMS MATRIX -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '7.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 7.0: Breakpoint Schedule &amp; Claims Matrix — Valuation Date</h2>
              <span class="unit-badge">As of {{ res.valuation_date }}</span>
            </div>

            <h3 class="sub-table-header">Breakpoint Equity Thresholds</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th style="width: 8%;">Tier</th>
                    <th style="width: 18%; text-align: right;">Start Equity Value</th>
                    <th style="width: 18%; text-align: right;">End Equity Value</th>
                    <th style="width: 18%; text-align: right;">Tranche Width</th>
                    <th style="width: 38%;">Economic Trigger Event / Claimant Classes</th>
                  </tr>
                </thead>
                <tbody>
                  @for (bp of res.valuation_breakpoints; track bp.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ bp.tier }}</span></td>
                      <td style="text-align: right;">{{ (bp.start_equity * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ bp.is_thereafter ? 'Thereafter' : ((bp.end_equity * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;">{{ bp.is_thereafter ? '—' : (((bp.width || (bp.end_equity - bp.start_equity)) * res.display_scale) | number:'1.2-2') }}</td>
                      <td>{{ bp.event_description || bp.claimants_description || bp.events_description }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Claims Sharing Percentage Matrix by Breakpoint Tier</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th style="width: 8%;">Tier</th>
                    <th style="width: 22%;">Equity Interval</th>
                    @for (sec of res.valuation_derived_securities; track sec.security) {
                      <th style="text-align: right;">{{ sec.security }}</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (cl of res.valuation_claims; track cl.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ cl.tier }}</span></td>
                      <td>
                        {{ (cl.from_equity !== undefined ? cl.from_equity : cl.start_equity || 0) * res.display_scale | number:'1.2-2' }} –
                        {{ cl.is_thereafter ? 'Thereafter' : (((cl.to_equity !== undefined ? cl.to_equity : cl.end_equity || 0) * res.display_scale) | number:'1.2-2') }}
                      </td>
                      @for (sec of res.valuation_derived_securities; track sec.security) {
                        <td style="text-align: right;">
                          {{ (cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) > 0 ? (((cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) * 100) | number:'1.1-1') + '%' : '—' }}
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 8.0: VALUATION OPM ALLOCATION -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '8.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 8.0: Option Pricing Method (OPM) Value Allocation — Valuation Date</h2>
              <span class="unit-badge">Concluded Equity: {{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</span>
            </div>

            <h3 class="sub-table-header">Black-Scholes Call Option Tranche Pricing</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Tranche</th>
                    <th style="text-align: right;">Lower Strike (Xk-1)</th>
                    <th style="text-align: right;">Upper Strike (Xk)</th>
                    <th style="text-align: right;">Call C(Xk-1)</th>
                    <th style="text-align: right;">Call C(Xk)</th>
                    <th style="text-align: right;">Incremental Value (ΔCk)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (tr of res.valuation_opm.tranches; track tr.tier) {
                    <tr>
                      <td><span class="badge tier">Tranche {{ tr.tier }}</span></td>
                      <td style="text-align: right;">{{ (tr.strike_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ tr.strike_high > 1e12 ? '∞' : ((tr.strike_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;">{{ (tr.call_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td style="text-align: right;">{{ tr.strike_high > 1e12 ? '0.00' : ((tr.call_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td style="text-align: right;"><strong>{{ (tr.incremental_call * res.display_scale) | number:'1.2-2' }}</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="5"><strong>Total Concluded Enterprise Equity Value</strong></td>
                    <td style="text-align: right;">{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Concluded Per-Share Fair Values</h3>
            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th>Security Class</th>
                    <th style="text-align: right;">Shares Out</th>
                    <th style="text-align: right;">Total Allocated Value</th>
                    @if (res.show_secondary_currency) {
                      <th style="text-align: right;">Value ({{ res.secondary_currency }})</th>
                    }
                    <th style="text-align: right;">Allocated %</th>
                    <th style="text-align: right;">Per-Share Fair Value</th>
                    @if (res.show_secondary_currency) {
                      <th style="text-align: right;">Per-Share ({{ res.secondary_currency }})</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (sec of res.valuation_derived_securities; track sec.security) {
                    <tr>
                      <td><strong>{{ sec.security }}</strong></td>
                      <td style="text-align: right;">{{ sec.shares | number:'1.0-0' }}</td>
                      <td style="text-align: right;">{{ ((res.valuation_opm.allocated_values[sec.security] || 0) * res.display_scale) | number:'1.2-2' }}</td>
                      @if (res.show_secondary_currency) {
                        <td style="text-align: right;">{{ ((res.valuation_opm.allocated_values[sec.security] || 0) * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                      }
                      <td style="text-align: right;">{{ ((res.valuation_opm.percent_allocations[sec.security] || 0) * 100) | number:'1.2-2' }}%</td>
                      <td style="text-align: right;">
                        <strong style="color: var(--teal);">{{ (res.valuation_opm.per_share_values[sec.security] || 0) | number:'1.2-2' }}</strong>
                      </td>
                      @if (res.show_secondary_currency) {
                        <td style="text-align: right;">
                          <strong>{{ ((res.valuation_opm.per_share_values[sec.security] || 0) * (res.secondary_fx_rate || 1.0)) | number:'1.2-2' }}</strong>
                        </td>
                      }
                    </tr>
                  }
                  <tr class="total-row">
                    <td><strong>Total Enterprise Equity Value</strong></td>
                    <td></td>
                    <td style="text-align: right;">{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</td>
                    @if (res.show_secondary_currency) {
                      <td style="text-align: right;">{{ (res.concluded_equity_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                    }
                    <td style="text-align: right;">100.00%</td>
                    <td></td>
                    @if (res.show_secondary_currency) {
                      <td></td>
                    }
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 9.0: VALUATION RISK-FREE RATE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '9.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 9.0: Risk-Free Rate Curve &amp; Interpolation — Valuation Date</h2>
              <span class="unit-badge">Term to Liquidity: {{ res.term_valuation | number:'1.2-2' }} Years</span>
            </div>

            @if (res.valuation_rf_analysis; as rf) {
              <div class="rf-summary-box">
                <p><strong>Curve Source:</strong> {{ rf.source_name }} (As-of {{ rf.as_of_date }})</p>
                <p><strong>Interpolated Annual Effective Rate:</strong> {{ rf.interpolated_annual_effective_rate * 100 | number:'1.4-4' }}%</p>
                <p><strong>Continuous Compounding Rate (rc):</strong> {{ rf.continuous_rate * 100 | number:'1.4-4' }}% (Formula: ln(1 + reff))</p>
                <p class="rf-meta"><em>{{ rf.interpolation_metadata }}</em></p>
              </div>

              @if (rf.curve_points && rf.curve_points.length > 0) {
                <h3 class="sub-table-header" style="margin-top: 18px;">Published Benchmark Yield Points</h3>
                <div class="spreadsheet-container">
                  <table class="spreadsheet-table">
                    <thead>
                      <tr>
                        <th>Tenor / Benchmark Maturity</th>
                        <th style="text-align: right;">Published Yield (%)</th>
                        <th style="text-align: right;">Maturity Term (Years)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (pt of rf.curve_points; track pt.tenor_name) {
                        <tr>
                          <td><strong>{{ pt.tenor_name }}</strong></td>
                          <td style="text-align: right;">{{ pt.rate_percent | number:'1.2-2' }}%</td>
                          <td style="text-align: right;">{{ pt.tenor_years | number:'1.2-2' }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            }
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 10.0: COMPARATIVE WATERFALL -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '10.0') {
          <div class="card accent-card">
            <div class="exhibit-title-bar">
              <h2>Exhibit 10.0: Comparative Liquidation Waterfall Schedule</h2>
              <span class="unit-badge">Scenario Exit Equity: {{ state.currencySymbol() }}{{ ((res.waterfall.applied_equity || res.waterfall.equity_value || 0) * res.display_scale) | number:'1.2-2' }}</span>
            </div>

            <div class="spreadsheet-container">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th rowspan="2" style="text-align: left; vertical-align: middle;">Security Class</th>
                    <th colspan="3" style="text-align: center; background: #234f7d; color: #fff;">Valuation Date Distribution</th>
                    <th colspan="3" style="text-align: center; background: #344653; color: #fff;">Calibration Date Structure</th>
                    <th colspan="2" style="text-align: center; background: #0b6b68; color: #fff;">Variance / Delta</th>
                  </tr>
                  <tr>
                    <th style="text-align: right;">Val Shares</th>
                    <th style="text-align: right;">Proceeds</th>
                    <th style="text-align: right;">Per Share</th>
                    <th style="text-align: right;">Cal Shares</th>
                    <th style="text-align: right;">Proceeds</th>
                    <th style="text-align: right;">Per Share</th>
                    <th style="text-align: right;">Δ Proceeds</th>
                    <th style="text-align: right;">% Change</th>
                  </tr>
                </thead>
                <tbody>
                  @for (w of res.comparative_waterfall; track w.security) {
                    <tr>
                      <td><strong>{{ w.security }}</strong></td>
                      <td style="text-align: right;">{{ w.val_shares ? (w.val_shares | number:'1.0-0') : '—' }}</td>
                      <td style="text-align: right;"><strong>{{ w.val_distribution !== undefined && w.val_distribution !== null ? ((w.val_distribution * res.display_scale) | number:'1.2-2') : '—' }}</strong></td>
                      <td style="text-align: right;">{{ w.val_per_share ? (w.val_per_share | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ w.cal_shares ? (w.cal_shares | number:'1.0-0') : '—' }}</td>
                      <td style="text-align: right;">{{ w.cal_distribution !== undefined && w.cal_distribution !== null ? ((w.cal_distribution * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">{{ w.cal_per_share ? (w.cal_per_share | number:'1.2-2') : '—' }}</td>
                      <td style="text-align: right;">
                        {{ w.change_in_distribution !== undefined && w.change_in_distribution !== null ? ((w.change_in_distribution * res.display_scale) | number:'1.2-2') : '—' }}
                      </td>
                      <td style="text-align: right;">
                        @if (w.cal_distribution && w.cal_distribution > 0 && w.change_in_distribution !== undefined && w.change_in_distribution !== null) {
                          <strong>{{ ((w.change_in_distribution / w.cal_distribution) * 100) | number:'+1.1-1' }}%</strong>
                        } @else if (w.change_in_distribution && w.change_in_distribution !== 0) {
                          <strong>New</strong>
                        } @else {
                          <span>—</span>
                        }
                      </td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td><strong>Total Distributed Proceeds</strong></td>
                    <td></td>
                    <td style="text-align: right;">{{ (res.waterfall.total_proceeds * res.display_scale) | number:'1.2-2' }}</td>
                    <td></td>
                    <td></td>
                    <td style="text-align: right;">{{ ((res.calibration_waterfall?.total_proceeds || 0) * res.display_scale) | number:'1.2-2' }}</td>
                    <td></td>
                    <td style="text-align: right;">
                      {{ ((res.waterfall.total_proceeds - (res.calibration_waterfall?.total_proceeds || 0)) * res.display_scale) | number:'1.2-2' }}
                    </td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .exhibits-wrapper {
      margin-top: 10px;
    }

    .kpi-sub {
      font-size: 11px;
      color: #78878b;
      margin-top: 4px;
      display: block;
    }

    .exhibit-pill-nav {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin: 18px 0 16px;
    }

    .exhibit-pill-nav button {
      background: #fff;
      border: 1px solid #d4dedc;
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 11.5px;
      font-weight: 600;
      color: #3b4e53;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .exhibit-pill-nav button:hover {
      border-color: var(--teal);
      color: var(--teal);
    }

    .exhibit-pill-nav button.active {
      background: var(--teal);
      color: #fff;
      border-color: var(--teal);
      box-shadow: 0 2px 6px rgba(11, 107, 104, 0.25);
    }

    .exhibit-title-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;
      flex-wrap: wrap;
      gap: 10px;
    }

    .unit-badge {
      font-size: 11.5px;
      color: #637377;
      font-style: italic;
    }

    .sub-table-header {
      font-size: 13px;
      font-weight: 700;
      color: #1e3a5f;
      margin: 12px 0 6px;
    }

    .rf-summary-box {
      background: #f7faf9;
      border: 1px solid #d9e5e3;
      border-radius: 8px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      font-size: 13px;
    }

    .rf-meta {
      margin-top: 6px;
      color: #6d7d81;
    }

    /* Cover Sheet View */
    .cover-sheet-view {
      padding: 36px 40px;
      background: linear-gradient(135deg, #17242b 0%, #29454c 60%, #0b6b68 100%);
      color: #ffffff;
      border-radius: 12px;
    }

    .cover-logo-wrap {
      margin-bottom: 20px;
    }

    .cover-logo-img {
      max-height: 48px;
      max-width: 200px;
      object-fit: contain;
      filter: brightness(0) invert(1);
    }

    .cover-eyebrow {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.15em;
      color: #a9d6cf;
      text-transform: uppercase;
      margin-bottom: 12px;
    }

    .cover-main-title {
      font-size: 26px;
      font-weight: 800;
      letter-spacing: -0.02em;
      color: #ffffff;
      margin: 0 0 8px;
    }

    .cover-sub-title {
      font-size: 14px;
      color: #e9f5f2;
      margin: 0 0 28px;
    }

    .cover-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      background: rgba(255, 255, 255, 0.08);
      padding: 20px;
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.18);
    }

    .cover-cell {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .cell-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #a9d6cf;
    }

    .cell-value {
      font-size: 16px;
      font-weight: 700;
      color: #ffffff;
    }

    .cell-value.highlight {
      color: #6ee7b7;
      font-size: 18px;
    }
  `]
})
export class ReportExhibitsComponent {
  readonly state = inject(ValuationStateService);
  readonly selectedExhibit = signal<string>('1.0');
}
