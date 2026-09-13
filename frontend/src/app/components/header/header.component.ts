import { Component, inject } from '@angular/core';
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
            <span class="logo-mark">CCA</span>
            <h1>Contingent Claims Analysis</h1>
          </div>
          <p class="subtitle">
            Option Pricing Model (OPM) & Backsolve Valuation Engine • 
            <strong>{{ state.request()?.company_name || 'TADO' }}</strong> 
            (Client: {{ state.request()?.client_name || 'S2G Investments' }})
          </p>
        </div>

        <div class="hero-right">
          <div class="badges">
            <span class="badge draft">{{ state.request()?.report_status || 'DRAFT' }}</span>
            <span class="badge confidential">CONFIDENTIAL</span>
          </div>

          <div class="action-buttons">
            <button class="btn" (click)="state.calculate()" [disabled]="state.loading()">
              <span>⚡</span> Run Valuation
            </button>
            <button class="btn excel" (click)="state.downloadExcel()" [disabled]="state.loading()">
              <span>📊</span> Excel (.xlsx)
            </button>
            <button class="btn pdf" (click)="state.downloadPdf()" [disabled]="state.loading()">
              <span>📑</span> PDF (Watermarked)
            </button>
            <button class="btn secondary" (click)="state.loadDefaultScenario()" [disabled]="state.loading()">
              <span>🔄</span> Reset Default
            </button>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs -->
      <nav class="nav-tabs">
        <button 
          [class.active]="state.activeTab() === 'inputs'"
          (click)="state.activeTab.set('inputs')">
          ⚙️ Engagement & Dates
        </button>
        <button 
          [class.active]="state.activeTab() === 'cap_tables'"
          (click)="state.activeTab.set('cap_tables')">
          📊 Capitalization Tables
        </button>
        <button 
          [class.active]="state.activeTab() === 'opm'"
          (click)="state.activeTab.set('opm')">
          📈 OPM Parameters
        </button>
        <button 
          [class.active]="state.activeTab() === 'holdings'"
          (click)="state.activeTab.set('holdings')">
          💼 Client Holdings
        </button>
        <button 
          [class.active]="state.activeTab() === 'capital_iq'"
          (click)="state.activeTab.set('capital_iq')">
          🏢 Capital IQ Bridge
        </button>
        <button 
          [class.active]="state.activeTab() === 'exhibits'"
          (click)="state.activeTab.set('exhibits')">
          📑 Report Exhibits (1.0 – 11.0)
        </button>
      </nav>
    </header>
  `,
  styles: [`
    .hero {
      background: linear-gradient(120deg, #17242b, #27424a 58%, #0b6b68);
      color: #fff;
      border-radius: 16px;
      padding: 24px 26px 0;
      margin-bottom: 22px;
      box-shadow: 0 12px 32px rgba(27, 49, 55, 0.18);
      position: relative;
      overflow: hidden;
    }

    .hero::after {
      content: "";
      position: absolute;
      width: 220px;
      height: 220px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 50%;
      right: -60px;
      top: -100px;
      box-shadow: 0 0 0 30px rgba(255, 255, 255, 0.03);
      pointer-events: none;
    }

    .hero-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
      flex-wrap: wrap;
      margin-bottom: 18px;
    }

    .branding-line {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 6px;
    }

    .logo-mark {
      background: var(--gold);
      color: #17242b;
      font-weight: 900;
      font-size: 13px;
      padding: 4px 8px;
      border-radius: 6px;
      letter-spacing: 0.05em;
    }

    .subtitle {
      color: #d6e4e2;
      font-size: 13px;
      margin-top: 4px;
    }

    .hero-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 12px;
    }

    .badges {
      display: flex;
      gap: 8px;
    }

    .action-buttons {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .nav-tabs {
      display: flex;
      gap: 4px;
      overflow-x: auto;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
      padding-top: 8px;
    }

    .nav-tabs button {
      background: transparent;
      border: 0;
      color: #cbdad8;
      font-size: 13px;
      font-weight: 600;
      padding: 10px 16px;
      border-radius: 8px 8px 0 0;
      cursor: pointer;
      transition: all 0.15s ease;
      white-space: nowrap;
    }

    .nav-tabs button:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.08);
    }

    .nav-tabs button.active {
      color: #fff;
      background: rgba(255, 255, 255, 0.16);
      border-bottom: 3px solid var(--gold);
    }
  `]
})
export class HeaderComponent {
  readonly state = inject(ValuationStateService);
}

