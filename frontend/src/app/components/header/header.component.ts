import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValuationStateService } from '../../services/valuation-state.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="hero">
      <div class="hero-content">
        <div class="hero-left">
          <div class="branding-line">
            @if (state.request()?.firm_logo_base64; as logo) {
              <div class="firm-logo-wrap">
                <img [src]="logo" alt="Firm Logo" class="firm-logo-img" />
              </div>
            } @else {
              <span class="logo-mark" aria-hidden="true">OPM</span>
            }
            <h1>Contingent Claims Analysis</h1>
          </div>
          <p class="subtitle">
            Option Pricing Method (OPM) &amp; Backsolve Engine • 
            <strong>{{ state.request()?.company_name || 'StellarAI Technologies, Inc.' }}</strong> 
            <span class="meta-separator">•</span>
            Client: <span>{{ state.request()?.client_name || 'S2G Investments' }}</span>
            <span class="meta-separator">•</span>
            As-of: <span>{{ state.request()?.valuation_date || '2026-06-30' }}</span>
          </p>
        </div>

        <div class="hero-right">
          <div class="badges-row">
            <!-- Reconciliation Status Badge -->
            @if (isReconciled()) {
              <span class="status-pill reconciled" title="All tranches and share classes reconciled to 100.00% of equity">
                <svg class="icon-svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.751.751 0 0 1 .018-1.042.751.751 0 0 1 1.042-.018L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"/>
                </svg>
                Status: Reconciled to 100.00%
              </span>
            } @else if (state.validationIssues().length > 0) {
              <span class="status-pill unreconciled" title="Validation issues need attention">
                <svg class="icon-svg" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z"/>
                </svg>
                {{ state.validationIssues().length }} Issue{{ state.validationIssues().length > 1 ? 's' : '' }}
              </span>
            }

            <span class="badge draft">{{ state.request()?.report_status || 'DRAFT' }}</span>
            <span class="badge confidential">CONFIDENTIAL</span>
          </div>

          <div class="action-buttons">
            <button 
              class="btn" 
              (click)="state.calculate()" 
              [disabled]="state.loading()"
              aria-label="Run valuation calculation model">
              <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M9.504.43a1.5 1.5 0 0 1 1.264.508l4.8 5.5a1.5 1.5 0 0 1-.17 2.134l-5.5 4.8A1.5 1.5 0 0 1 8.834 13.5H3.5a1.5 1.5 0 0 1-1.5-1.5V6.666a1.5 1.5 0 0 1 .43-1.06l5.5-5.5A1.5 1.5 0 0 1 8.5.1h1.004ZM8 3.5a.5.5 0 0 0-.5.5v3.5h-3.5a.5.5 0 0 0 0 1h4a.5.5 0 0 0 .5-.5V4a.5.5 0 0 0-.5-.5Z"/>
              </svg>
              <span>Run Valuation</span>
            </button>

            <button 
              class="btn excel" 
              (click)="state.downloadExcel()" 
              [disabled]="state.loading()"
              aria-label="Download auditable multi-sheet Excel model">
              <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M5.18 4.616a.5.5 0 0 1 .704.064L8 7.293l2.116-2.613a.5.5 0 1 1 .768.64L8.64 8l2.244 2.68a.5.5 0 0 1-.768.64L8 8.707l-2.116 2.613a.5.5 0 0 1-.768-.64L7.36 8 5.116 5.32a.5.5 0 0 1 .064-.704Z"/>
                <path d="M4 0h5.293A1 1 0 0 1 10 .293l3.707 3.707A1 1 0 0 1 14 4.707V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2Zm5.5 1.5v2a1 1 0 0 0 1 1h2l-3-3ZM3 2v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5.5H10A2.5 2.5 0 0 1 7.5 3V1H4a1 1 0 0 0-1 1Z"/>
              </svg>
              <span>Excel (.xlsx)</span>
            </button>

            <button 
              class="btn pdf" 
              (click)="state.downloadPdf()" 
              [disabled]="state.loading()"
              aria-label="Download executive 12-page watermarked PDF report book">
              <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M14 14V4.5L9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2ZM9.5 3A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5v2ZM4.5 9a.5.5 0 0 1 .5-.5h6a.5.5 0 0 1 0 1H5a.5.5 0 0 1-.5-.5Zm0 2a.5.5 0 0 1 .5-.5h6a.5.5 0 0 1 0 1H5a.5.5 0 0 1-.5-.5Z"/>
              </svg>
              <span>PDF Report Book</span>
            </button>

            <button 
              class="btn secondary" 
              (click)="state.loadDefaultScenario()" 
              [disabled]="state.loading()"
              aria-label="Reset scenario inputs to default benchmark">
              <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
              </svg>
              <span>Reset</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs -->
      <nav class="nav-tabs" aria-label="Main application tabs">
        <button 
          [class.active]="state.activeTab() === 'inputs'"
          (click)="state.activeTab.set('inputs')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'inputs'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M8 4.754a3.246 3.246 0 1 0 0 6.492 3.246 3.246 0 0 0 0-6.492ZM5.754 8a2.246 2.246 0 1 1 4.492 0 2.246 2.246 0 0 1-4.492 0Z"/>
            <path d="M9.796 1.343c-.527-1.79-3.065-1.79-3.592 0l-.094.319a.873.873 0 0 1-1.255.52l-.292-.16c-1.64-.892-3.433.902-2.54 2.541l.159.292a.873.873 0 0 1-.52 1.255l-.319.094c-1.79.527-1.79 3.065 0 3.592l.319.094a.873.873 0 0 1 .52 1.255l-.16.292c-.892 1.64.901 3.434 2.541 2.54l.292-.159a.873.873 0 0 1 1.255.52l.094.319c.527 1.79 3.065 1.79 3.592 0l.094-.319a.873.873 0 0 1 1.255-.52l.292.16c1.64.893 3.434-.902 2.54-2.541l-.159-.292a.873.873 0 0 1 .52-1.255l.319-.094c1.79-.527 1.79-3.065 0-3.592l-.319-.094a.873.873 0 0 1-.52-1.255l.16-.292c.893-1.64-.902-3.433-2.541-2.54l-.292.159a.873.873 0 0 1-1.255-.52l-.094-.319Z"/>
          </svg>
          <span>Engagement &amp; Dates</span>
        </button>

        <button 
          [class.active]="state.activeTab() === 'cap_tables'"
          (click)="state.activeTab.set('cap_tables')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'cap_tables'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M0 2a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V2Zm15 2h-4v3h4V4Zm0 4h-4v3h4V8Zm0 4h-4v3h3a1 1 0 0 0 1-1v-2Zm-5 3v-3H6v3h4Zm-5 0v-3H1v2a1 1 0 0 0 1 1h3Zm-4-4h4V8H1v3Zm0-4h4V4H1v3Zm5-3v3h4V4H6Zm4 4H6v3h4V8Z"/>
          </svg>
          <span>Capitalization Tables</span>
        </button>

        <button 
          [class.active]="state.activeTab() === 'opm'"
          (click)="state.activeTab.set('opm')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'opm'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M0 0h1v15h15v1H0V0Zm14.817 3.113a.5.5 0 0 1 .07.704l-4.5 5.5a.5.5 0 0 1-.74.037L7.06 6.767l-3.656 5.027a.5.5 0 0 1-.808-.588l4-5.5a.5.5 0 0 1 .758-.06l2.609 2.61 4.15-5.073a.5.5 0 0 1 .704-.07Z"/>
          </svg>
          <span>OPM Parameters</span>
        </button>

        <button 
          [class.active]="state.activeTab() === 'holdings'"
          (click)="state.activeTab.set('holdings')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'holdings'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M6.5 1A1.5 1.5 0 0 0 5 2.5V3H1.5A1.5 1.5 0 0 0 0 4.5v8A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5v-8A1.5 1.5 0 0 0 14.5 3H11v-.5A1.5 1.5 0 0 0 9.5 1h-3Zm0 1h3a.5.5 0 0 1 .5.5V3H6v-.5a.5.5 0 0 1 .5-.5ZM1 4.5a.5.5 0 0 1 .5-.5h13a.5.5 0 0 1 .5.5v1.642a80.37 80.37 0 0 0-7.5.358 80.364 80.364 0 0 0-7.5-.358V4.5Zm0 2.674v5.326a.5.5 0 0 0 .5.5h13a.5.5 0 0 0 .5-.5V7.174a81.56 81.56 0 0 1-7.5.355 81.564 81.564 0 0 1-7.5-.355Z"/>
          </svg>
          <span>Client Holdings</span>
        </button>

        <button 
          [class.active]="state.activeTab() === 'capital_iq'"
          (click)="state.activeTab.set('capital_iq')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'capital_iq'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M4 2.5a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5h-7a.5.5 0 0 1-.5-.5v-2Zm0 5a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5h-7a.5.5 0 0 1-.5-.5v-2Zm0 5a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5h-7a.5.5 0 0 1-.5-.5v-2ZM2 1a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V1Z"/>
          </svg>
          <span>Capital IQ Bridge</span>
        </button>

        <button 
          [class.active]="state.activeTab() === 'exhibits'"
          (click)="state.activeTab.set('exhibits')"
          role="tab"
          [attr.aria-selected]="state.activeTab() === 'exhibits'">
          <svg class="tab-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5Zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2Z"/>
          </svg>
          <span>Report Exhibits (1.0 – 11.0)</span>
        </button>
      </nav>
    </header>
  `,
  styles: [`
    .hero {
      background: linear-gradient(125deg, #121c22 0%, #1f343a 52%, #08615e 100%);
      color: #ffffff;
      border-radius: 14px;
      padding: 20px 22px 0;
      margin-bottom: 20px;
      box-shadow: 0 8px 24px rgba(18, 28, 34, 0.16);
      position: relative;
      overflow: hidden;
    }

    .hero::after {
      content: "";
      position: absolute;
      width: 200px;
      height: 200px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 50%;
      right: -50px;
      top: -80px;
      box-shadow: 0 0 0 30px rgba(255, 255, 255, 0.025);
      pointer-events: none;
    }

    .hero-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }

    .branding-line {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 5px;
    }

    .branding-line h1 {
      font-size: 26px;
      font-weight: 700;
      color: #ffffff;
      margin: 0;
      line-height: 1.2;
      letter-spacing: -0.01em;
    }

    .firm-logo-wrap {
      display: flex;
      align-items: center;
    }

    .firm-logo-img {
      max-height: 36px;
      max-width: 140px;
      object-fit: contain;
      filter: brightness(0) invert(1);
    }

    .logo-mark {
      background: var(--gold);
      color: #121c22;
      font-weight: 900;
      font-size: 11.5px;
      padding: 3px 7px;
      border-radius: 4px;
      letter-spacing: 0.06em;
    }

    .subtitle {
      color: #dce9e7;
      font-size: 12px;
      margin-top: 4px;
      line-height: 1.45;
    }

    .subtitle strong {
      color: #ffffff;
    }

    .meta-separator {
      margin: 0 6px;
      opacity: 0.6;
    }

    .hero-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 10px;
    }

    .badges-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .icon-svg {
      width: 13px;
      height: 13px;
      flex-shrink: 0;
    }

    .action-buttons {
      display: flex;
      gap: 7px;
      flex-wrap: wrap;
    }

    .btn-icon {
      width: 13px;
      height: 13px;
      flex-shrink: 0;
    }

    .nav-tabs {
      display: flex;
      gap: 3px;
      overflow-x: auto;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
      padding-top: 6px;
    }

    .nav-tabs button {
      background: transparent;
      border: 0;
      color: #c0d3d0;
      font-size: 12.5px;
      font-weight: 600;
      padding: 9px 14px;
      border-radius: 6px 6px 0 0;
      cursor: pointer;
      transition: all 0.14s ease;
      white-space: nowrap;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    .tab-icon {
      width: 13px;
      height: 13px;
      opacity: 0.8;
    }

    .nav-tabs button:hover {
      color: #ffffff;
      background: rgba(255, 255, 255, 0.09);
    }

    .nav-tabs button.active {
      color: #ffffff;
      background: rgba(255, 255, 255, 0.18);
      border-bottom: 3px solid var(--gold);
    }

    .nav-tabs button.active .tab-icon {
      opacity: 1;
      color: var(--gold);
    }
  `]
})
export class HeaderComponent {
  readonly state = inject(ValuationStateService);

  readonly isReconciled = computed(() => {
    const res = this.state.response();
    if (!res) return false;
    if (this.state.validationIssues().length > 0) return false;
    // Check if total allocated value matches concluded equity value closely
    const concluded = res.concluded_equity_value;
    const allocated = res.valuation_opm?.total_allocated;
    if (concluded > 0 && allocated > 0) {
      return Math.abs(concluded - allocated) / concluded < 0.001;
    }
    return true;
  });
}
