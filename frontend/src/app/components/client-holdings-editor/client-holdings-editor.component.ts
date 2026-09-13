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
            <h2>Client Portfolio Holdings (Exhibit 1.0)</h2>
            <p class="section-desc">
              Specify fund investment positions, unit share counts, and historical cost bases to generate MOIC metrics and fair value summaries.
            </p>
          </div>

          <button class="btn" (click)="state.addHoldingRow()">
            ➕ Add Position
          </button>
        </div>

        <!-- Holdings Table -->
        <div class="spreadsheet-container">
          <table class="spreadsheet-table">
            <thead>
              <tr>
                <th style="width: 25%;">Fund / Legal Entity</th>
                <th style="width: 25%;">Security Class</th>
                <th style="width: 20%;">Units / Shares Held</th>
                <th style="width: 20%;">Invested Cost ({{ state.currencySymbol() }})</th>
                <th style="width: 10%; text-align: center;">Action</th>
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
                      placeholder="e.g. S2G Fund I" />
                  </td>
                  <td>
                    <select [(ngModel)]="h.security" (change)="state.calculate()">
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
                      placeholder="0" />
                  </td>
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="h.cost" 
                      (change)="state.calculate()" 
                      placeholder="0.00" />
                  </td>
                  <td style="text-align: center;">
                    <button class="delete-btn" (click)="state.removeHoldingRow($index)" title="Remove position">
                      ✕
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Live Subtotal Feedback -->
        @if (state.response()?.holdings; as hs) {
          <div class="kpi-grid">
            <div class="kpi-card">
              <span class="kpi-label">Total Portfolio Cost</span>
              <span class="kpi-val">{{ state.currencySymbol() }}{{ hs.total_cost | number:'1.2-2' }}</span>
            </div>
            <div class="kpi-card">
              <span class="kpi-label">Total Concluded Fair Value</span>
              <span class="kpi-val">{{ state.currencySymbol() }}{{ hs.total_value | number:'1.2-2' }}</span>
            </div>
            <div class="kpi-card">
              <span class="kpi-label">Consolidated Portfolio MOIC</span>
              <span class="kpi-val">{{ (hs.consolidated_moic || 0) | number:'1.2-2' }}x</span>
            </div>
          </div>

          <!-- Fund Breakdown -->
          <div class="fund-summary-table-wrap">
            <h3>Subtotals by Investment Vehicle</h3>
            <table class="spreadsheet-table">
              <thead>
                <tr>
                  <th>Vehicle</th>
                  <th style="text-align: right;">Total Units</th>
                  <th style="text-align: right;">Cost Basis ({{ state.currencySymbol() }})</th>
                  <th style="text-align: right;">Fair Value ({{ state.currencySymbol() }})</th>
                  <th style="text-align: right;">Fund MOIC</th>
                </tr>
              </thead>
              <tbody>
                @for (f of hs.fund_subtotals; track f.fund) {
                  <tr>
                    <td><strong>{{ f.fund }}</strong></td>
                    <td style="text-align: right;">{{ f.total_units | number:'1.0-0' }}</td>
                    <td style="text-align: right;">{{ state.currencySymbol() }}{{ f.total_cost | number:'1.2-2' }}</td>
                    <td style="text-align: right;">{{ state.currencySymbol() }}{{ f.total_value | number:'1.2-2' }}</td>
                    <td style="text-align: right;"><strong>{{ (f.moic || 0) | number:'1.2-2' }}x</strong></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .section-header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }

    .section-desc {
      color: #687386;
      font-size: 13px;
    }

    .delete-btn {
      background: transparent;
      border: 0;
      color: #a44;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
      padding: 3px 6px;
      border-radius: 4px;
    }

    .delete-btn:hover {
      background: #fde8e8;
    }

    .fund-summary-table-wrap {
      margin-top: 18px;
    }
  `]
})
export class ClientHoldingsEditorComponent {
  readonly state = inject(ValuationStateService);
}

