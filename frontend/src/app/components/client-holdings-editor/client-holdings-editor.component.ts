import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValuationStateService } from '../../services/valuation-state.service';

@Component({
  selector: 'app-client-holdings-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state.request(); as req) {
      <div class="card accent-card">
        <div class="section-header-row">
          <div>
            <h2>Client Portfolio Holdings (Exhibit 1.0 Ledger)</h2>
            <p class="section-desc">
              Specify investment vehicle positions, share counts, and cost basis to compute multi-fund ownership, MOIC, and concluded fair values.
            </p>
          </div>

          <button class="btn" (click)="state.addHoldingRow()" aria-label="Add investment position">
            <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
            </svg>
            <span>Add Position</span>
          </button>
        </div>

        <!-- Positions Ledger -->
        <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Holdings table spreadsheet">
          <table class="spreadsheet-table">
            <thead>
              <tr>
                <th scope="col" style="width: 26%;">Fund / Legal Investment Vehicle</th>
                <th scope="col" style="width: 26%;">Security Class</th>
                <th scope="col" style="width: 20%; text-align: right;">Units / Shares Held</th>
                <th scope="col" style="width: 20%; text-align: right;">Invested Cost Basis ({{ state.currencySymbol() }})</th>
                <th scope="col" style="width: 8%; text-align: center;">Action</th>
              </tr>
            </thead>
            <tbody>
              @for (h of req.holdings; track $index) {
                <tr>
                  <td>
                    <input 
                      type="text" 
                      [(ngModel)]="h.fund" 
                      (change)="state.calculate()" 
                      aria-label="Fund name"
                      placeholder="e.g. S2G Fund I" />
                  </td>
                  <td>
                    <select [(ngModel)]="h.security" (change)="state.calculate()" aria-label="Target security class">
                      @for (secName of state.availableValuationSecurities(); track secName) {
                        <option [value]="secName">{{ secName }}</option>
                      }
                    </select>
                  </td>
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="h.units" 
                      (change)="state.calculate()" 
                      class="text-right tabular"
                      aria-label="Units held"
                      placeholder="0" />
                  </td>
                  <td>
                    <div class="input-wrapper">
                      <span class="input-prefix">{{ state.currencySymbol() }}</span>
                      <input 
                        type="number" 
                        step="100" 
                        [(ngModel)]="h.cost" 
                        (change)="state.calculate()" 
                        class="has-prefix text-right tabular"
                        aria-label="Invested cost"
                        placeholder="0.00" />
                    </div>
                  </td>
                  <td style="text-align: center;">
                    <button 
                      class="delete-btn" 
                      (click)="state.removeHoldingRow($index)" 
                      title="Remove holding position"
                      aria-label="Delete position row">
                      ✕
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Live Portfolio Metrics Feedback -->
        @if (state.response()?.holdings; as hs) {
          <div class="kpi-grid" style="margin-top: 20px;">
            <div class="kpi-card">
              <span class="kpi-label">Total Invested Cost</span>
              <span class="kpi-val tabular">{{ state.currencySymbol() }}{{ hs.total_cost | number:'1.2-2' }}</span>
            </div>
            <div class="kpi-card">
              <span class="kpi-label">Concluded Portfolio Fair Value</span>
              <span class="kpi-val tabular">{{ state.currencySymbol() }}{{ hs.total_value | number:'1.2-2' }}</span>
            </div>
            <div class="kpi-card">
              <span class="kpi-label">Consolidated Portfolio MOIC</span>
              <span class="kpi-val tabular">{{ (hs.consolidated_moic || 0) | number:'1.2-2' }}x</span>
            </div>
          </div>

          <!-- Fund Breakdown Table -->
          @if (hs.fund_subtotals && hs.fund_subtotals.length > 0) {
            <div class="fund-summary-table-wrap">
              <h3>Subtotals by Investment Vehicle</h3>
              <div class="spreadsheet-container">
                <table class="spreadsheet-table">
                  <thead>
                    <tr>
                      <th scope="col" style="width: 30%;">Fund Name</th>
                      <th scope="col" style="width: 20%; text-align: right;">Total Units</th>
                      <th scope="col" style="width: 25%; text-align: right;">Total Cost Basis</th>
                      <th scope="col" style="width: 25%; text-align: right;">Concluded Value</th>
                      <th scope="col" style="width: 15%; text-align: right;">Fund MOIC</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (f of hs.fund_subtotals; track f.fund) {
                      <tr>
                        <td><strong>{{ f.fund }}</strong></td>
                        <td class="text-right tabular">{{ f.total_units | number:'1.0-0' }}</td>
                        <td class="text-right tabular">{{ state.currencySymbol() }}{{ f.total_cost | number:'1.2-2' }}</td>
                        <td class="text-right tabular"><strong>{{ state.currencySymbol() }}{{ f.total_value | number:'1.2-2' }}</strong></td>
                        <td class="text-right tabular"><strong>{{ (f.moic || 0) | number:'1.2-2' }}x</strong></td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          }
        }
      </div>
    }
  `,
  styles: [`
    .section-header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }

    .btn-icon {
      width: 13px;
      height: 13px;
    }

    .text-right {
      text-align: right;
    }

    .delete-btn {
      background: transparent;
      border: 0;
      color: #94a3b8;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
      padding: 2px 6px;
      border-radius: 4px;
      transition: color 0.12s, background 0.12s;
    }

    .delete-btn:hover {
      color: var(--danger);
      background: var(--danger-bg);
    }

    .fund-summary-table-wrap {
      margin-top: 18px;
    }
  `]
})
export class ClientHoldingsEditorComponent {
  readonly state = inject(ValuationStateService);
}
