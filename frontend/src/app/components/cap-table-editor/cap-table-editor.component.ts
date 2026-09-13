import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValuationStateService } from '../../services/valuation-state.service';
import { SecurityInput } from '../../models/valuation.models';

@Component({
  selector: 'app-cap-table-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state.request(); as req) {
      <div class="card accent-card">
        <div class="table-header-row">
          <div>
            <h2>15-Column Capitalization Structure</h2>
            <p class="section-desc">
              Manage share counts, seniority rankings, liquidation preferences, conversion prices, and dividend terms.
            </p>
          </div>

          <div class="table-actions">
            <div class="date-selector-tabs">
              <button 
                [class.active]="selectedTable() === 'calibration'"
                (click)="selectedTable.set('calibration')">
                Calibration Date ({{ req.calibration_date }})
              </button>
              <button 
                [class.active]="selectedTable() === 'valuation'"
                (click)="selectedTable.set('valuation')">
                Valuation Date ({{ req.valuation_date }})
              </button>
            </div>

            @if (selectedTable() === 'valuation') {
              <button class="btn secondary copy-btn" (click)="state.copyCalibrationToValuation()">
                📋 Copy from Calibration
              </button>
            }

            <button class="btn add-btn" (click)="state.addSecurityRow(selectedTable())">
              ➕ Add Security
            </button>
          </div>
        </div>

        <!-- Spreadsheet Grid -->
        <div class="spreadsheet-container">
          <table class="spreadsheet-table">
            <thead>
              <tr>
                <th style="width: 11%;">Security</th>
                <th style="width: 9%;">Subtype</th>
                <th style="width: 7%;">Shares</th>
                <th style="width: 7%;">Exercise Price</th>
                <th style="width: 7%;">Orig. Issue Price</th>
                <th style="width: 7%;">Conv. Price</th>
                <th style="width: 5%;">Liq. Mult.</th>
                <th style="width: 6%;">Participation</th>
                <th style="width: 6%;">Cap</th>
                <th style="width: 4%;">Seniority</th>
                <th style="width: 8%;">Issue Date</th>
                <th style="width: 6%;">Div. Rate %</th>
                <th style="width: 7%;">Compounding</th>
                <th style="width: 6%;">Div. Paid</th>
                <th style="width: 4%; text-align: center;">Action</th>
              </tr>
            </thead>
            <tbody>
              @for (sec of currentRows(); track $index) {
                <tr>
                  <!-- Security Name -->
                  <td>
                    <input 
                      type="text" 
                      [(ngModel)]="sec.security" 
                      (change)="onRowChange()"
                      [class.invalid]="!sec.security.trim()" />
                  </td>

                  <!-- Subtype -->
                  <td>
                    <select [(ngModel)]="sec.security_subtype" (change)="onSubtypeChange(sec)">
                      <option value="Preferred Stock">Preferred Stock</option>
                      <option value="Common Stock">Common Stock</option>
                      <option value="Option">Option</option>
                      <option value="Warrant">Warrant</option>
                    </select>
                  </td>

                  <!-- Number of Shares -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.shares" 
                      (change)="onRowChange()"
                      [class.invalid]="sec.shares <= 0" />
                  </td>

                  <!-- Exercise Price -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.exercise_price" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype === 'Preferred Stock' || sec.security_subtype === 'Common Stock'"
                      [placeholder]="isOptionWarrant(sec) ? '0.00' : 'N/A'" />
                  </td>

                  <!-- Original Issue Price -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.original_issue_price" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.00' : 'N/A'" />
                  </td>

                  <!-- Conversion Price -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.conversion_price" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.00' : 'N/A'" />
                  </td>

                  <!-- Liquidation Multiplier -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.liquidation_multiplier" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '1.0' : 'N/A'" />
                  </td>

                  <!-- Participation -->
                  <td>
                    <select 
                      [(ngModel)]="sec.participation" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'">
                      <option value="No">No</option>
                      <option value="Yes">Yes</option>
                      <option value="NA">NA</option>
                    </select>
                  </td>

                  <!-- Max Cap -->
                  <td>
                    <input 
                      type="text" 
                      [(ngModel)]="sec.max_participation_cap" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock' || sec.participation !== 'Yes'"
                      [placeholder]="sec.participation === 'Yes' ? 'e.g. 3x or No Cap' : 'N/A'" />
                  </td>

                  <!-- Seniority -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.seniority" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '1' : 'N/A'" />
                  </td>

                  <!-- Issue Date -->
                  <td>
                    <input 
                      type="date" 
                      [(ngModel)]="sec.issue_date" 
                      (change)="onRowChange()" />
                  </td>

                  <!-- Annual Dividend Rate -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.dividend_rate" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      placeholder="0.0" />
                  </td>

                  <!-- Compounding -->
                  <td>
                    <select 
                      [(ngModel)]="sec.compounding_convention" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'">
                      <option value="Annual">Annual</option>
                      <option value="Semi-Annual">Semi-Annual</option>
                      <option value="Quarterly">Quarterly</option>
                      <option value="Monthly">Monthly</option>
                      <option value="Simple">Simple</option>
                    </select>
                  </td>

                  <!-- Dividends Paid To Date -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.dividends_paid_to_date" 
                      (change)="onRowChange()"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      placeholder="0.0" />
                  </td>

                  <!-- Delete Row -->
                  <td style="text-align: center;">
                    <button class="delete-btn" (click)="state.removeSecurityRow(selectedTable(), $index)" title="Remove class">
                      ✕
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
  styles: [`
    .table-header-row {
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

    .table-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    .date-selector-tabs {
      display: flex;
      background: #eef3f2;
      border-radius: 8px;
      padding: 3px;
      border: 1px solid #dce4e2;
    }

    .date-selector-tabs button {
      background: transparent;
      border: 0;
      padding: 6px 12px;
      font-size: 12px;
      font-weight: 600;
      color: #5a6b6f;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .date-selector-tabs button.active {
      background: #fff;
      color: var(--teal);
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
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
      transition: background 0.15s;
    }

    .delete-btn:hover {
      background: #fde8e8;
    }
  `]
})
export class CapTableEditorComponent {
  readonly state = inject(ValuationStateService);
  readonly selectedTable = signal<'calibration' | 'valuation'>('calibration');

  currentRows(): SecurityInput[] {
    const req = this.state.request();
    if (!req) return [];
    return this.selectedTable() === 'calibration' ? req.calibration_securities : req.valuation_securities;
  }

  isOptionWarrant(sec: SecurityInput): boolean {
    return sec.security_subtype === 'Option' || sec.security_subtype === 'Warrant';
  }

  onSubtypeChange(sec: SecurityInput): void {
    if (sec.security_subtype === 'Common Stock') {
      sec.exercise_price = null;
      sec.original_issue_price = null;
      sec.conversion_price = null;
      sec.participation = 'NA';
      sec.max_participation_cap = 'NA';
      sec.seniority = null;
      sec.dividend_rate = 0.0;
    } else if (sec.security_subtype === 'Option' || sec.security_subtype === 'Warrant') {
      sec.original_issue_price = null;
      sec.conversion_price = null;
      sec.participation = 'NA';
      sec.max_participation_cap = 'NA';
      sec.seniority = null;
      sec.dividend_rate = 0.0;
    } else if (sec.security_subtype === 'Preferred Stock') {
      sec.exercise_price = null;
      if (!sec.original_issue_price) sec.original_issue_price = 1000.0;
      if (!sec.conversion_price) sec.conversion_price = 1000.0;
      if (!sec.seniority) sec.seniority = 1;
    }
    this.onRowChange();
  }

  onRowChange(): void {
    this.state.calculate();
  }
}

