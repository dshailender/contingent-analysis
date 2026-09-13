import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValuationStateService } from '../../services/valuation-state.service';

interface DistributionSegment {
  security: string;
  percent: number;
  type: 'pref' | 'common' | 'option' | 'warrant';
}

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
            <span class="kpi-val tabular">{{ state.currencySymbol() }}{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">Transaction round as-of {{ res.calibration_date }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Valuation Concluded Equity</span>
            <span class="kpi-val tabular">{{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">Measurement date as-of {{ res.valuation_date }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Client Portfolio Fair Value</span>
            <span class="kpi-val tabular">{{ state.currencySymbol() }}{{ (res.holdings.total_value * res.display_scale) | number:'1.2-2' }}</span>
            <span class="kpi-sub">Cost basis: {{ state.currencySymbol() }}{{ (res.holdings.total_cost * res.display_scale) | number:'1.2-2' }}</span>
          </div>

          <div class="kpi-card">
            <span class="kpi-label">Consolidated Portfolio MOIC</span>
            <span class="kpi-val tabular">{{ (res.holdings.consolidated_moic || 0) | number:'1.2-2' }}x</span>
            <span class="kpi-sub">Multiple on Invested Capital</span>
          </div>
        </div>

        <!-- Proportional Equity Distribution Visualizer -->
        @if (distributionSegments().length > 0) {
          <div class="distribution-visual-box no-print">
            <div class="dist-header">
              <span class="dist-title">Concluded Equity Distribution by Share Class</span>
              <span class="dist-subtitle">Visualized from OPM allocation percentages (100.00% Total)</span>
            </div>
            <div class="equity-distribution-bar" role="progressbar" aria-label="Equity allocation distribution">
              @for (seg of distributionSegments(); track seg.security) {
                @if (seg.percent > 0.005) {
                  <div 
                    class="dist-segment" 
                    [ngClass]="seg.type"
                    [style.width.%]="seg.percent * 100"
                    [title]="seg.security + ': ' + (seg.percent * 100 | number:'1.1-1') + '%'">
                    {{ seg.security }} ({{ seg.percent * 100 | number:'1.0-0' }}%)
                  </div>
                }
              }
            </div>
          </div>
        }

        <!-- Exhibit Navigation Pills -->
        <div class="exhibit-pill-nav no-print" role="tablist" aria-label="Valuation report exhibit selection">
          <button [class.active]="selectedExhibit() === 'Cover'" (click)="selectedExhibit.set('Cover')" role="tab">Cover Sheet</button>
          <button [class.active]="selectedExhibit() === 'Index'" (click)="selectedExhibit.set('Index')" role="tab">Executive Index</button>
          <button [class.active]="selectedExhibit() === '1.0'" (click)="selectedExhibit.set('1.0')" role="tab">Ex 1.0: Holdings</button>
          <button [class.active]="selectedExhibit() === '2.0'" (click)="selectedExhibit.set('2.0')" role="tab">Ex 2.0: Cal Cap Table</button>
          <button [class.active]="selectedExhibit() === '3.0'" (click)="selectedExhibit.set('3.0')" role="tab">Ex 3.0: Cal Breakpoints</button>
          <button [class.active]="selectedExhibit() === '4.0'" (click)="selectedExhibit.set('4.0')" role="tab">Ex 4.0: Cal OPM Backsolve</button>
          <button [class.active]="selectedExhibit() === '5.0'" (click)="selectedExhibit.set('5.0')" role="tab">Ex 5.0: Cal Risk-Free</button>
          <button [class.active]="selectedExhibit() === '6.0'" (click)="selectedExhibit.set('6.0')" role="tab">Ex 6.0: Val Cap Table</button>
          <button [class.active]="selectedExhibit() === '7.0'" (click)="selectedExhibit.set('7.0')" role="tab">Ex 7.0: Val Breakpoints</button>
          <button [class.active]="selectedExhibit() === '8.0'" (click)="selectedExhibit.set('8.0')" role="tab">Ex 8.0: Val OPM Allocation</button>
          <button [class.active]="selectedExhibit() === '9.0'" (click)="selectedExhibit.set('9.0')" role="tab">Ex 9.0: Val Risk-Free</button>
          <button [class.active]="selectedExhibit() === '10.0'" (click)="selectedExhibit.set('10.0')" role="tab">Ex 10.0: Comparative Waterfall</button>
        </div>

        <!-- ========================================================================= -->
        <!-- COVER SHEET -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === 'Cover') {
          <div id="reportCover" class="report-page cover-page">
            <div class="cover-inner">
              @if (res.firm_logo_base64) {
                <img [src]="res.firm_logo_base64" alt="Firm Logo" class="cover-firm-logo" />
              }
              <div class="cover-analysis-label">CONTINGENT CLAIMS ANALYSIS</div>
              <div class="cover-rule"></div>
              <h1 id="coverCompanyName">{{ res.company_name }}</h1>
              <div class="cover-client">
                <span>PREPARED FOR</span>
                <b>{{ res.client_name }}</b>
              </div>
              <div class="cover-valuation-date">
                <span>VALUATION DATE</span>
                <b>{{ res.valuation_date }}</b>
              </div>
              @if (res.report_status) {
                <div id="coverReportStatus" class="cover-report-status">{{ res.report_status }}</div>
              }
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- INDEX OF EXHIBITS -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === 'Index') {
          <div id="reportIndex" class="report-page index-page">
            <div class="index-topline"></div>
            <div class="index-header-grid">
              <div class="index-header-left">
                <div class="index-client">{{ res.company_name }}</div>
                <div class="index-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="index-asof">As of {{ res.valuation_date }}</div>
                <div class="index-label">Index of Exhibits</div>
              </div>
              <div class="index-header-right">
                <div class="index-word">Index</div>
                @if (res.report_status) {
                  <div id="indexReportStatus" class="index-status">{{ res.report_status }}</div>
                }
              </div>
            </div>
            <div class="index-blue-rule"></div>

            <div class="index-table-wrap">
              <table class="index-table">
                <thead>
                  <tr>
                    <th scope="col">Exhibit</th>
                    <th scope="col" style="text-align: right;">Page</th>
                  </tr>
                </thead>
                <tbody>
                  <tr><td>Exhibit 1.0: Client Portfolio Holdings &amp; Concluded Values</td><td style="text-align: right;">3</td></tr>
                  <tr><td>Exhibit 2.0: Capitalization Structure &amp; Security Terms (Calibration Date)</td><td style="text-align: right;">4</td></tr>
                  <tr><td>Exhibit 3.0: Breakpoint Schedule &amp; Claims Matrix (Calibration Date)</td><td style="text-align: right;">5</td></tr>
                  <tr><td>Exhibit 4.0: OPM Backsolve Tranches &amp; Solved Enterprise Value (Calibration Date)</td><td style="text-align: right;">6</td></tr>
                  <tr><td>Exhibit 5.0: Risk-Free Rate Curve &amp; Tenor Interpolation (Calibration Date)</td><td style="text-align: right;">7</td></tr>
                  <tr><td>Exhibit 6.0: Capitalization Structure &amp; Security Terms (Valuation Date)</td><td style="text-align: right;">8</td></tr>
                  <tr><td>Exhibit 7.0: Breakpoint Schedule &amp; Claims Matrix (Valuation Date)</td><td style="text-align: right;">9</td></tr>
                  <tr><td>Exhibit 8.0: OPM Tranche Allocation &amp; Concluded Per-Share Values (Valuation Date)</td><td style="text-align: right;">10</td></tr>
                  <tr><td>Exhibit 9.0: Risk-Free Rate Curve &amp; Tenor Interpolation (Valuation Date)</td><td style="text-align: right;">11</td></tr>
                  <tr><td>Exhibit 10.0: Comparative Liquidation Preference Waterfall &amp; Delta Analysis</td><td style="text-align: right;">12</td></tr>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 1.0: Client Portfolio Holdings &amp; Concluded Value</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }})</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 1.0</div>
                <div class="prh-page">Page 3 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Holdings exhibit table">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Fund / Legal Entity</th>
                    <th scope="col">Security Class</th>
                    <th scope="col" style="text-align: right;">Units Held</th>
                    <th scope="col" style="text-align: right;">Cost Basis</th>
                    <th scope="col" style="text-align: right;">Fair Value / Sh</th>
                    <th scope="col" style="text-align: right;">Concluded Value</th>
                    @if (res.show_secondary_currency) {
                      <th scope="col" style="text-align: right;">Value ({{ res.secondary_currency }})</th>
                    }
                    <th scope="col" style="text-align: right;">Class Own %</th>
                    <th scope="col" style="text-align: right;">FD Own %</th>
                    <th scope="col" style="text-align: right;">MOIC</th>
                  </tr>
                </thead>
                <tbody>
                  @for (h of res.holdings.items; track $index) {
                    <tr>
                      <td><strong>{{ h.fund }}</strong></td>
                      <td>{{ h.security }}</td>
                      <td class="text-right tabular">{{ h.units | number:'1.0-0' }}</td>
                      <td class="text-right tabular">{{ (h.cost * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ h.fair_value_per_share | number:'1.2-2' }}</td>
                      <td class="text-right tabular"><strong>{{ (h.concluded_fair_value * res.display_scale) | number:'1.2-2' }}</strong></td>
                      @if (res.show_secondary_currency) {
                        <td class="text-right tabular">{{ (h.concluded_fair_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                      }
                      <td class="text-right tabular">{{ (h.class_ownership_pct * 100) | number:'1.2-2' }}%</td>
                      <td class="text-right tabular">{{ (h.fully_diluted_ownership_pct * 100) | number:'1.2-2' }}%</td>
                      <td class="text-right tabular"><strong>{{ (h.moic || 0) | number:'1.2-2' }}x</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="3"><strong>Total Client Portfolio</strong></td>
                    <td class="text-right tabular">{{ (res.holdings.total_cost * res.display_scale) | number:'1.2-2' }}</td>
                    <td></td>
                    <td class="text-right tabular"><strong>{{ (res.holdings.total_value * res.display_scale) | number:'1.2-2' }}</strong></td>
                    @if (res.show_secondary_currency) {
                      <td class="text-right tabular"><strong>{{ (res.holdings.total_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</strong></td>
                    }
                    <td></td>
                    <td></td>
                    <td class="text-right tabular"><strong>{{ (res.holdings.consolidated_moic || 0) | number:'1.2-2' }}x</strong></td>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 2.0: Capitalization Table &amp; Terms — Calibration Date</div>
                <div class="prh-date">As of {{ res.calibration_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }})</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 2.0</div>
                <div class="prh-page">Page 4 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Calibration cap table">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Security Class</th>
                    <th scope="col">Subtype</th>
                    <th scope="col" style="text-align: right;">Shares</th>
                    <th scope="col" style="text-align: right;">Issue Price</th>
                    <th scope="col" style="text-align: right;">Liq. Mult</th>
                    <th scope="col" style="text-align: right;">Seniority</th>
                    <th scope="col" style="text-align: right;">Participation</th>
                    <th scope="col" style="text-align: right;">Pref / Share</th>
                    <th scope="col" style="text-align: right;">Total Preference</th>
                    <th scope="col" style="text-align: right;">FD Shares</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of res.calibration_derived_securities; track s.security) {
                    <tr>
                      <td><strong>{{ s.security }}</strong></td>
                      <td>{{ s.security_subtype }}</td>
                      <td class="text-right tabular">{{ s.shares | number:'1.0-0' }}</td>
                      <td class="text-right tabular">{{ s.original_issue_price > 0 ? (s.original_issue_price | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.liquidation_multiplier | number:'1.2-2' }}x</td>
                      <td class="text-right tabular">{{ s.seniority < 999 ? s.seniority : '—' }}</td>
                      <td class="text-right">{{ s.participation }}</td>
                      <td class="text-right tabular">{{ s.liquidation_preference_per_share > 0 ? (s.liquidation_preference_per_share | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.total_liquidation_preference > 0 ? ((s.total_liquidation_preference * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.fully_diluted_shares | number:'1.0-0' }}</td>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 3.0: Breakpoint Schedule &amp; Claims Matrix — Calibration Date</div>
                <div class="prh-date">As of {{ res.calibration_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }})</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 3.0</div>
                <div class="prh-page">Page 5 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <h3 class="sub-table-header">Breakpoint Equity Thresholds</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Calibration breakpoint thresholds">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col" style="width: 8%;">Tier</th>
                    <th scope="col" style="width: 18%; text-align: right;">Start Equity Value</th>
                    <th scope="col" style="width: 18%; text-align: right;">End Equity Value</th>
                    <th scope="col" style="width: 18%; text-align: right;">Tranche Width</th>
                    <th scope="col" style="width: 38%;">Economic Trigger Event / Claimant Classes</th>
                  </tr>
                </thead>
                <tbody>
                  @for (bp of res.calibration_breakpoints; track bp.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ bp.tier }}</span></td>
                      <td class="text-right tabular">{{ (bp.start_equity * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ bp.is_thereafter ? 'Thereafter' : ((bp.end_equity * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular">{{ bp.is_thereafter ? '—' : (((bp.width || (bp.end_equity - bp.start_equity)) * res.display_scale) | number:'1.2-2') }}</td>
                      <td>{{ bp.event_description || bp.claimants_description || bp.events_description }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Claims Sharing Percentage Matrix by Breakpoint Tier</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Calibration claims sharing matrix">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col" style="width: 8%;">Tier</th>
                    <th scope="col" style="width: 22%;">Equity Interval</th>
                    @for (sec of res.calibration_derived_securities; track sec.security) {
                      <th scope="col" style="text-align: right;">{{ sec.security }}</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (cl of res.calibration_claims; track cl.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ cl.tier }}</span></td>
                      <td class="tabular">
                        {{ (cl.from_equity !== undefined ? cl.from_equity : cl.start_equity || 0) * res.display_scale | number:'1.2-2' }} –
                        {{ cl.is_thereafter ? 'Thereafter' : (((cl.to_equity !== undefined ? cl.to_equity : cl.end_equity || 0) * res.display_scale) | number:'1.2-2') }}
                      </td>
                      @for (sec of res.calibration_derived_securities; track sec.security) {
                        <td class="text-right tabular">
                          {{ (cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) > 0 ? (((cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) * 100) | number:'1.1-1') + '%' : '—' }}
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="combined-footnotes">
              <div>
                <h4>Breakpoint Mechanics &amp; Trigger Events</h4>
                <div>
                  Breakpoints represent equity values where preferred liquidation preferences are satisfied, participation caps are met, or preferred converts to common stock.
                </div>
              </div>
              <div>
                <h4>Contractual Entitlement Reconciliation</h4>
                <div>
                  In each tranche, marginal proceeds are allocated in strict accordance with contractual preferences, reconciling to <b>100.00%</b> across all classes.
                </div>
              </div>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 4.0: CALIBRATION OPM BACKSOLVE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '4.0') {
          <div class="card accent-card">
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 4.0: OPM Backsolve Tranches &amp; Solved Equity — Calibration Date</div>
                <div class="prh-date">As of {{ res.calibration_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }}) • Term: {{ res.term_calibration | number:'1.2-2' }}y • Vol: {{ res.calibration_opm.volatility * 100 | number:'1.1-1' }}% • Rf: {{ res.rf_calibration_effective | number:'1.2-2' }}%</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 4.0</div>
                <div class="prh-page">Page 6 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <h3 class="sub-table-header">Black-Scholes Call Option Tranche Pricing</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Calibration option tranches">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Tranche</th>
                    <th scope="col" style="text-align: right;">Lower Strike (Xk-1)</th>
                    <th scope="col" style="text-align: right;">Upper Strike (Xk)</th>
                    <th scope="col" style="text-align: right;">Call C(Xk-1)</th>
                    <th scope="col" style="text-align: right;">Call C(Xk)</th>
                    <th scope="col" style="text-align: right;">Incremental Value (ΔCk)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (tr of res.calibration_opm.tranches; track tr.tier) {
                    <tr>
                      <td><span class="badge tier">Tranche {{ tr.tier }}</span></td>
                      <td class="text-right tabular">{{ (tr.strike_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ tr.strike_high > 1e12 ? '∞' : ((tr.strike_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular">{{ (tr.call_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ tr.strike_high > 1e12 ? '0.00' : ((tr.call_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular"><strong>{{ (tr.incremental_call * res.display_scale) | number:'1.2-2' }}</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="5"><strong>Total Solved Enterprise Equity Value</strong></td>
                    <td class="text-right tabular"><strong>{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Security-by-Security Concluded Values</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Calibration securities allocation">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Security Class</th>
                    <th scope="col" style="text-align: right;">Shares Out</th>
                    <th scope="col" style="text-align: right;">Total Allocated Value</th>
                    <th scope="col" style="text-align: right;">Allocated %</th>
                    <th scope="col" style="text-align: right;">Per-Share Value</th>
                  </tr>
                </thead>
                <tbody>
                  @for (sec of res.calibration_derived_securities; track sec.security) {
                    <tr>
                      <td><strong>{{ sec.security }}</strong></td>
                      <td class="text-right tabular">{{ sec.shares | number:'1.0-0' }}</td>
                      <td class="text-right tabular">{{ ((res.calibration_opm.allocated_values[sec.security] || 0) * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ ((res.calibration_opm.percent_allocations[sec.security] || 0) * 100) | number:'1.2-2' }}%</td>
                      <td class="text-right tabular">
                        <strong>{{ (res.calibration_opm.per_share_values[sec.security] || 0) | number:'1.2-2' }}</strong>
                      </td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td><strong>Total Solved Enterprise Equity</strong></td>
                    <td></td>
                    <td class="text-right tabular"><strong>{{ (res.calibration_solved_equity * res.display_scale) | number:'1.2-2' }}</strong></td>
                    <td class="text-right tabular">100.00%</td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="combined-footnotes">
              <div>
                <h4>Black-Scholes Tranche Pricing</h4>
                <div>
                  Tranche values represent call option spreads C(Xk-1) - C(Xk) priced using the Black-Scholes formula with continuous risk-free rate and volatility.
                </div>
              </div>
              <div>
                <h4>Backsolve Calibration Target</h4>
                <div>
                  Total enterprise equity value is solved so that the calibrated round price equals <b>{{ state.currencySymbol() }}{{ (state.request()?.transaction_price || 0) | number:'1.2-2' }}</b> per share.
                </div>
              </div>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 5.0: CALIBRATION RISK-FREE RATE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '5.0') {
          <div class="card accent-card">
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 5.0: Risk-Free Rate Curve &amp; Interpolation — Calibration Date</div>
                <div class="prh-date">As of {{ res.calibration_date }}</div>
                <div class="prh-units">Term to Exit: {{ res.term_calibration | number:'1.2-2' }} Years</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 5.0</div>
                <div class="prh-page">Page 7 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            @if (res.calibration_rf_analysis; as rf) {
              <div class="rf-summary-box">
                <p><strong>Curve Source:</strong> {{ rf.source_name }} (As-of {{ rf.as_of_date }})</p>
                <p><strong>Interpolated Annual Effective Rate:</strong> <span class="tabular">{{ rf.interpolated_annual_effective_rate * 100 | number:'1.4-4' }}%</span></p>
                <p><strong>Continuous Compounding Rate (rc):</strong> <span class="tabular">{{ rf.continuous_rate * 100 | number:'1.4-4' }}%</span> (Formula: ln(1 + reff))</p>
                <p class="rf-meta"><em>{{ rf.interpolation_metadata }}</em></p>
              </div>

              @if (rf.curve_points && rf.curve_points.length > 0) {
                <h3 class="sub-table-header" style="margin-top: 18px;">Published Benchmark Yield Points</h3>
                <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Published yield points">
                  <table class="spreadsheet-table">
                    <thead>
                      <tr>
                        <th scope="col">Tenor / Benchmark Maturity</th>
                        <th scope="col" style="text-align: right;">Published Yield (%)</th>
                        <th scope="col" style="text-align: right;">Maturity Term (Years)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (pt of rf.curve_points; track pt.tenor_name) {
                        <tr>
                          <td><strong>{{ pt.tenor_name }}</strong></td>
                          <td class="text-right tabular">{{ pt.rate_percent | number:'1.2-2' }}%</td>
                          <td class="text-right tabular">{{ pt.tenor_years | number:'1.2-2' }}</td>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 6.0: Capitalization Table &amp; Terms — Valuation Date</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }})</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 6.0</div>
                <div class="prh-page">Page 8 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Valuation cap table">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Security Class</th>
                    <th scope="col">Subtype</th>
                    <th scope="col" style="text-align: right;">Shares</th>
                    <th scope="col" style="text-align: right;">Issue Price</th>
                    <th scope="col" style="text-align: right;">Conv. Price</th>
                    <th scope="col" style="text-align: right;">Seniority</th>
                    <th scope="col" style="text-align: right;">Participation</th>
                    <th scope="col" style="text-align: right;">Total Preference</th>
                    <th scope="col" style="text-align: right;">Conv. Threshold</th>
                    <th scope="col" style="text-align: right;">FD Shares</th>
                  </tr>
                </thead>
                <tbody>
                  @for (s of res.valuation_derived_securities; track s.security) {
                    <tr>
                      <td><strong>{{ s.security }}</strong></td>
                      <td>{{ s.security_subtype }}</td>
                      <td class="text-right tabular">{{ s.shares | number:'1.0-0' }}</td>
                      <td class="text-right tabular">{{ s.original_issue_price > 0 ? (s.original_issue_price | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.conversion_price > 0 ? (s.conversion_price | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.seniority < 999 ? s.seniority : '—' }}</td>
                      <td class="text-right">{{ s.participation }}</td>
                      <td class="text-right tabular">{{ s.total_liquidation_preference > 0 ? ((s.total_liquidation_preference * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.conversion_threshold_equity > 0 ? ((s.conversion_threshold_equity * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ s.fully_diluted_shares | number:'1.0-0' }}</td>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 7.0: Breakpoint Schedule &amp; Claims Matrix — Valuation Date</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }})</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 7.0</div>
                <div class="prh-page">Page 9 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <h3 class="sub-table-header">Breakpoint Equity Thresholds</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Valuation breakpoints">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col" style="width: 8%;">Tier</th>
                    <th scope="col" style="width: 18%; text-align: right;">Start Equity Value</th>
                    <th scope="col" style="width: 18%; text-align: right;">End Equity Value</th>
                    <th scope="col" style="width: 18%; text-align: right;">Tranche Width</th>
                    <th scope="col" style="width: 38%;">Economic Trigger Event / Claimant Classes</th>
                  </tr>
                </thead>
                <tbody>
                  @for (bp of res.valuation_breakpoints; track bp.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ bp.tier }}</span></td>
                      <td class="text-right tabular">{{ (bp.start_equity * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ bp.is_thereafter ? 'Thereafter' : ((bp.end_equity * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular">{{ bp.is_thereafter ? '—' : (((bp.width || (bp.end_equity - bp.start_equity)) * res.display_scale) | number:'1.2-2') }}</td>
                      <td>{{ bp.event_description || bp.claimants_description || bp.events_description }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Claims Sharing Percentage Matrix by Breakpoint Tier</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Valuation claims sharing matrix">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col" style="width: 8%;">Tier</th>
                    <th scope="col" style="width: 22%;">Equity Interval</th>
                    @for (sec of res.valuation_derived_securities; track sec.security) {
                      <th scope="col" style="text-align: right;">{{ sec.security }}</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (cl of res.valuation_claims; track cl.tier) {
                    <tr>
                      <td><span class="badge tier">Tier {{ cl.tier }}</span></td>
                      <td class="tabular">
                        {{ (cl.from_equity !== undefined ? cl.from_equity : cl.start_equity || 0) * res.display_scale | number:'1.2-2' }} –
                        {{ cl.is_thereafter ? 'Thereafter' : (((cl.to_equity !== undefined ? cl.to_equity : cl.end_equity || 0) * res.display_scale) | number:'1.2-2') }}
                      </td>
                      @for (sec of res.valuation_derived_securities; track sec.security) {
                        <td class="text-right tabular">
                          {{ (cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) > 0 ? (((cl.sharing_percentages?.[sec.security] || cl.percent_claims?.[sec.security] || 0) * 100) | number:'1.1-1') + '%' : '—' }}
                        </td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="combined-footnotes">
              <div>
                <h4>Breakpoint Liquidation Thresholds</h4>
                <div>
                  Represents sequential thresholds of enterprise equity value where preferred liquidation preferences and dividend accruals are fully satisfied.
                </div>
              </div>
              <div>
                <h4>Incremental Claims Sharing</h4>
                <div>
                  Marginal proceeds within each tier are divided among entitled classes, reconciling strictly to <b>100.00%</b> across all classes.
                </div>
              </div>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 8.0: VALUATION OPM ALLOCATION -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '8.0') {
          <div class="card accent-card">
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 8.0: OPM Value Allocation Matrix — Valuation Date</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }}) • Concluded Equity: {{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 8.0</div>
                <div class="prh-page">Page 10 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <h3 class="sub-table-header">Black-Scholes Call Option Tranche Pricing</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Valuation option tranches">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Tranche</th>
                    <th scope="col" style="text-align: right;">Lower Strike (Xk-1)</th>
                    <th scope="col" style="text-align: right;">Upper Strike (Xk)</th>
                    <th scope="col" style="text-align: right;">Call C(Xk-1)</th>
                    <th scope="col" style="text-align: right;">Call C(Xk)</th>
                    <th scope="col" style="text-align: right;">Incremental Value (ΔCk)</th>
                  </tr>
                </thead>
                <tbody>
                  @for (tr of res.valuation_opm.tranches; track tr.tier) {
                    <tr>
                      <td><span class="badge tier">Tranche {{ tr.tier }}</span></td>
                      <td class="text-right tabular">{{ (tr.strike_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ tr.strike_high > 1e12 ? '∞' : ((tr.strike_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular">{{ (tr.call_low * res.display_scale) | number:'1.2-2' }}</td>
                      <td class="text-right tabular">{{ tr.strike_high > 1e12 ? '0.00' : ((tr.call_high * res.display_scale) | number:'1.2-2') }}</td>
                      <td class="text-right tabular"><strong>{{ (tr.incremental_call * res.display_scale) | number:'1.2-2' }}</strong></td>
                    </tr>
                  }
                  <tr class="total-row">
                    <td colspan="5"><strong>Total Concluded Enterprise Equity Value</strong></td>
                    <td class="text-right tabular"><strong>{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3 class="sub-table-header" style="margin-top: 20px;">Concluded Per-Share Fair Values</h3>
            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Valuation concluded per-share fair values">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th scope="col">Security Class</th>
                    <th scope="col" style="text-align: right;">Shares Out</th>
                    <th scope="col" style="text-align: right;">Total Allocated Value</th>
                    @if (res.show_secondary_currency) {
                      <th scope="col" style="text-align: right;">Value ({{ res.secondary_currency }})</th>
                    }
                    <th scope="col" style="text-align: right;">Allocated %</th>
                    <th scope="col" style="text-align: right;">Per-Share Value</th>
                    @if (res.show_secondary_currency) {
                      <th scope="col" style="text-align: right;">Per-Share ({{ res.secondary_currency }})</th>
                    }
                  </tr>
                </thead>
                <tbody>
                  @for (sec of res.valuation_derived_securities; track sec.security) {
                    <tr>
                      <td><strong>{{ sec.security }}</strong></td>
                      <td class="text-right tabular">{{ sec.shares | number:'1.0-0' }}</td>
                      <td class="text-right tabular">{{ ((res.valuation_opm.allocated_values[sec.security] || 0) * res.display_scale) | number:'1.2-2' }}</td>
                      @if (res.show_secondary_currency) {
                        <td class="text-right tabular">{{ ((res.valuation_opm.allocated_values[sec.security] || 0) * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</td>
                      }
                      <td class="text-right tabular">{{ ((res.valuation_opm.percent_allocations[sec.security] || 0) * 100) | number:'1.2-2' }}%</td>
                      <td class="text-right tabular">
                        <strong style="color: var(--teal);">{{ (res.valuation_opm.per_share_values[sec.security] || 0) | number:'1.2-2' }}</strong>
                      </td>
                      @if (res.show_secondary_currency) {
                        <td class="text-right tabular">
                          <strong>{{ ((res.valuation_opm.per_share_values[sec.security] || 0) * (res.secondary_fx_rate || 1.0)) | number:'1.2-2' }}</strong>
                        </td>
                      }
                    </tr>
                  }
                  <tr class="total-row">
                    <td><strong>Total Enterprise Equity Value</strong></td>
                    <td></td>
                    <td class="text-right tabular"><strong>{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</strong></td>
                    @if (res.show_secondary_currency) {
                      <td class="text-right tabular"><strong>{{ (res.concluded_equity_value * (res.secondary_fx_rate || 1.0) * res.display_scale) | number:'1.2-2' }}</strong></td>
                    }
                    <td class="text-right tabular">100.00%</td>
                    <td></td>
                    @if (res.show_secondary_currency) {
                      <td></td>
                    }
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="combined-footnotes">
              <div>
                <h4>OPM Tranche Value Allocation</h4>
                <div>
                  Each security's concluded fair value is the sum of its contractual shares of call option spreads across all breakpoint tranches.
                </div>
              </div>
              <div>
                <h4>Statutory Fair Value Reconciliation</h4>
                <div>
                  Concluded equity of <b>{{ state.currencySymbol() }}{{ (res.concluded_equity_value * res.display_scale) | number:'1.2-2' }}</b> is reconciled to <b>100.00%</b> across all common, preferred, and derivative classes.
                </div>
              </div>
            </div>
          </div>
        }

        <!-- ========================================================================= -->
        <!-- EXHIBIT 9.0: VALUATION RISK-FREE RATE -->
        <!-- ========================================================================= -->
        @if (selectedExhibit() === '9.0') {
          <div class="card accent-card">
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 9.0: Risk-Free Rate Curve &amp; Interpolation — Valuation Date</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Term to Liquidity: {{ res.term_valuation | number:'1.2-2' }} Years</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 9.0</div>
                <div class="prh-page">Page 11 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            @if (res.valuation_rf_analysis; as rf) {
              <div class="rf-summary-box">
                <p><strong>Curve Source:</strong> {{ rf.source_name }} (As-of {{ rf.as_of_date }})</p>
                <p><strong>Interpolated Annual Effective Rate:</strong> <span class="tabular">{{ rf.interpolated_annual_effective_rate * 100 | number:'1.4-4' }}%</span></p>
                <p><strong>Continuous Compounding Rate (rc):</strong> <span class="tabular">{{ rf.continuous_rate * 100 | number:'1.4-4' }}%</span> (Formula: ln(1 + reff))</p>
                <p class="rf-meta"><em>{{ rf.interpolation_metadata }}</em></p>
              </div>

              @if (rf.curve_points && rf.curve_points.length > 0) {
                <h3 class="sub-table-header" style="margin-top: 18px;">Published Benchmark Yield Points</h3>
                <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Published yield points">
                  <table class="spreadsheet-table">
                    <thead>
                      <tr>
                        <th scope="col">Tenor / Benchmark Maturity</th>
                        <th scope="col" style="text-align: right;">Published Yield (%)</th>
                        <th scope="col" style="text-align: right;">Maturity Term (Years)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (pt of rf.curve_points; track pt.tenor_name) {
                        <tr>
                          <td><strong>{{ pt.tenor_name }}</strong></td>
                          <td class="text-right tabular">{{ pt.rate_percent | number:'1.2-2' }}%</td>
                          <td class="text-right tabular">{{ pt.tenor_years | number:'1.2-2' }}</td>
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
            <div class="page-report-header">
              <div class="prh-left">
                <div class="prh-client">{{ res.company_name }}</div>
                <div class="prh-purpose">{{ res.report_purpose || 'Option Pricing Method (OPM) Analysis' }}</div>
                <div class="prh-exhibit">Exhibit 10.0: Comparative Liquidation Waterfall Schedule</div>
                <div class="prh-date">As of {{ res.valuation_date }}</div>
                <div class="prh-units">Amounts in {{ res.display_units }} ({{ res.report_currency }}) • Applied Exit Equity: {{ state.currencySymbol() }}{{ ((res.waterfall.applied_equity || res.waterfall.equity_value || 0) * res.display_scale) | number:'1.2-2' }}</div>
              </div>
              <div class="prh-right">
                <div class="prh-exhibit-no">Exhibit 10.0</div>
                <div class="prh-page">Page 12 of 12</div>
                @if (res.report_status) {
                  <div class="prh-status">{{ res.report_status }}</div>
                }
              </div>
            </div>

            <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Comparative liquidation waterfall schedule">
              <table class="spreadsheet-table">
                <thead>
                  <tr>
                    <th rowspan="2" scope="col" style="text-align: left; vertical-align: middle;">Security Class</th>
                    <th colspan="3" scope="colgroup" style="text-align: center; background: #1c3d5a; color: #ffffff;">Valuation Date Distribution</th>
                    <th colspan="3" scope="colgroup" style="text-align: center; background: #264349; color: #ffffff;">Calibration Date Structure</th>
                    <th colspan="2" scope="colgroup" style="text-align: center; background: #08615e; color: #ffffff;">Variance / Delta</th>
                  </tr>
                  <tr>
                    <th scope="col" style="text-align: right;">Val Shares</th>
                    <th scope="col" style="text-align: right;">Proceeds</th>
                    <th scope="col" style="text-align: right;">Per Share</th>
                    <th scope="col" style="text-align: right;">Cal Shares</th>
                    <th scope="col" style="text-align: right;">Proceeds</th>
                    <th scope="col" style="text-align: right;">Per Share</th>
                    <th scope="col" style="text-align: right;">Δ Proceeds</th>
                    <th scope="col" style="text-align: right;">% Change</th>
                  </tr>
                </thead>
                <tbody>
                  @for (w of res.comparative_waterfall; track w.security) {
                    <tr>
                      <td><strong>{{ w.security }}</strong></td>
                      <td class="text-right tabular">{{ w.val_shares ? (w.val_shares | number:'1.0-0') : '—' }}</td>
                      <td class="text-right tabular"><strong>{{ w.val_distribution !== undefined && w.val_distribution !== null ? ((w.val_distribution * res.display_scale) | number:'1.2-2') : '—' }}</strong></td>
                      <td class="text-right tabular">{{ w.val_per_share ? (w.val_per_share | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ w.cal_shares ? (w.cal_shares | number:'1.0-0') : '—' }}</td>
                      <td class="text-right tabular">{{ w.cal_distribution !== undefined && w.cal_distribution !== null ? ((w.cal_distribution * res.display_scale) | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">{{ w.cal_per_share ? (w.cal_per_share | number:'1.2-2') : '—' }}</td>
                      <td class="text-right tabular">
                        {{ w.change_in_distribution !== undefined && w.change_in_distribution !== null ? ((w.change_in_distribution * res.display_scale) | number:'1.2-2') : '—' }}
                      </td>
                      <td class="text-right tabular">
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
                    <td class="text-right tabular"><strong>{{ (res.waterfall.total_proceeds * res.display_scale) | number:'1.2-2' }}</strong></td>
                    <td></td>
                    <td></td>
                    <td class="text-right tabular"><strong>{{ ((res.calibration_waterfall?.total_proceeds || 0) * res.display_scale) | number:'1.2-2' }}</strong></td>
                    <td></td>
                    <td class="text-right tabular">
                      <strong>{{ ((res.waterfall.total_proceeds - (res.calibration_waterfall?.total_proceeds || 0)) * res.display_scale) | number:'1.2-2' }}</strong>
                    </td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="combined-footnotes">
              <div>
                <h4>Comparative Liquidation Structure</h4>
                <div>
                  Evaluates contractual distribution outcomes across security classes at hypothetical liquidity events, comparing Calibration vs Valuation cap table terms.
                </div>
              </div>
              <div>
                <h4>Preference Priority &amp; Dilution Analysis</h4>
                <div>
                  Variance in distributions isolates the impact of subsequent financing round preference layering and equity dilution on early-stage stakeholders.
                </div>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .exhibits-wrapper {
      margin-top: 8px;
    }

    .kpi-sub {
      font-size: 11px;
      color: #63757a;
      margin-top: 3px;
      display: block;
    }

    .distribution-visual-box {
      background: #fbfdfc;
      border: 1px solid #d8e5e2;
      border-radius: 10px;
      padding: 14px 18px;
      margin: 14px 0 16px;
    }

    .dist-header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      flex-wrap: wrap;
      gap: 8px;
    }

    .dist-title {
      font-size: 12px;
      font-weight: 700;
      color: var(--slate);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .dist-subtitle {
      font-size: 11px;
      color: #697c82;
      font-style: italic;
    }

    .exhibit-pill-nav {
      display: flex;
      gap: 5px;
      flex-wrap: wrap;
      margin: 16px 0 16px;
    }

    .exhibit-pill-nav button {
      background: #ffffff;
      border: 1px solid #d0dedb;
      padding: 6px 12px;
      border-radius: 18px;
      font-size: 11.5px;
      font-weight: 600;
      color: #2b3e43;
      cursor: pointer;
      transition: all 0.13s ease;
    }

    .exhibit-pill-nav button:hover {
      border-color: var(--teal);
      color: var(--teal);
      background: #f4faf8;
    }

    .exhibit-pill-nav button.active {
      background: var(--teal);
      color: #ffffff;
      border-color: var(--teal);
      box-shadow: 0 2px 6px rgba(8, 97, 94, 0.25);
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
      color: #55686d;
      font-style: italic;
    }

    .sub-table-header {
      font-size: 13px;
      font-weight: 700;
      color: #17364b;
      margin: 14px 0 6px;
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

    .text-right {
      text-align: right;
    }
  `]
})
export class ReportExhibitsComponent {
  readonly state = inject(ValuationStateService);
  readonly selectedExhibit = signal<string>('1.0');

  readonly distributionSegments = computed<DistributionSegment[]>(() => {
    const res = this.state.response();
    if (!res || !res.valuation_derived_securities || !res.valuation_opm?.percent_allocations) {
      return [];
    }
    return res.valuation_derived_securities.map(s => {
      const pct = res.valuation_opm.percent_allocations[s.security] || 0;
      let t: 'pref' | 'common' | 'option' | 'warrant' = 'pref';
      if (s.security_subtype === 'Common Stock') t = 'common';
      else if (s.security_subtype === 'Option') t = 'option';
      else if (s.security_subtype === 'Warrant') t = 'warrant';
      return {
        security: s.security,
        percent: pct,
        type: t
      };
    });
  });
}
