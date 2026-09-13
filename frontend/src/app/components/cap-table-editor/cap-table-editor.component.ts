import { Component, inject, signal, computed } from '@angular/core';
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
            <h2>15-Column Capitalization Structure Ledger</h2>
            <p class="section-desc">
              Institutional security terms: share quantities, contractual liquidation preferences, seniority ranking, participation caps, and dividend accrual parameters.
            </p>
          </div>

          <div class="table-actions-group">
            <div class="date-selector-tabs" role="tablist" aria-label="Capitalization structure selection">
              <button 
                [class.active]="selectedTable() === 'calibration'"
                (click)="selectedTable.set('calibration')"
                role="tab"
                [attr.aria-selected]="selectedTable() === 'calibration'">
                Calibration Date ({{ req.calibration_date }})
              </button>
              <button 
                [class.active]="selectedTable() === 'valuation'"
                (click)="selectedTable.set('valuation')"
                role="tab"
                [attr.aria-selected]="selectedTable() === 'valuation'">
                Valuation Date ({{ req.valuation_date }})
              </button>
            </div>

            @if (selectedTable() === 'valuation') {
              <button 
                class="btn secondary copy-btn" 
                (click)="state.copyCalibrationToValuation()"
                title="Copy all securities and contractual terms from the calibration cap table">
                <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1v-1z"/>
                  <path d="M9.5 1a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-.5.5h-3a.5.5 0 0 1-.5-.5v-1a.5.5 0 0 1 .5-.5h3zm-3-1A1.5 1.5 0 0 0 5 1.5v1A1.5 1.5 0 0 0 6.5 4h3A1.5 1.5 0 0 0 11 2.5v-1A1.5 1.5 0 0 0 9.5 0h-3z"/>
                </svg>
                <span>Copy from Calibration</span>
              </button>
            }

            <button 
              class="btn add-btn" 
              (click)="state.addSecurityRow(selectedTable())"
              aria-label="Add new security class to capitalization table">
              <svg class="btn-icon" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
              </svg>
              <span>Add Security</span>
            </button>
          </div>
        </div>

        <!-- 15-Column Spreadsheet Grid -->
        <div class="spreadsheet-container" tabindex="0" role="region" aria-label="Capitalization table spreadsheet">
          <table class="security-input-table">
            <thead>
              <tr>
                <th scope="col">Security Class</th>
                <th scope="col">Subtype</th>
                <th scope="col" class="text-right">Shares</th>
                <th scope="col" class="text-right">Strike Price</th>
                <th scope="col" class="text-right">Issue Price</th>
                <th scope="col" class="text-right">Conv. Price</th>
                <th scope="col" class="text-right">Liq. Mult.</th>
                <th scope="col">Participation</th>
                <th scope="col">Part. Cap</th>
                <th scope="col" class="text-right">Seniority</th>
                <th scope="col">Issue Date</th>
                <th scope="col" class="text-right">Div. %</th>
                <th scope="col">Compounding</th>
                <th scope="col" class="text-right">Div. Paid</th>
                <th scope="col" style="text-align: center;">Action</th>
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
                      (focus)="onSecurityNameFocus(sec.security)"
                      (change)="onSecurityNameChange(sec)"
                      aria-label="Security class name"
                      placeholder="e.g. Series B Preferred"
                      [class.invalid]="!sec.security.trim()" />
                  </td>

                  <!-- Subtype -->
                  <td>
                    <select [(ngModel)]="sec.security_subtype" (change)="onSubtypeChange(sec)" aria-label="Security subtype">
                      <option value="Preferred Stock">Preferred Stock</option>
                      <option value="Common Stock">Common Stock</option>
                      <option value="Option">Option</option>
                      <option value="Warrant">Warrant</option>
                    </select>
                  </td>

                  <!-- Shares Outstanding -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.shares" 
                      (change)="onRowChange()"
                      aria-label="Shares outstanding count"
                      class="text-right"
                      placeholder="0"
                      [class.invalid]="sec.shares <= 0" />
                  </td>

                  <!-- Exercise / Strike Price (Options / Warrants) -->
                  <td>
                    <input 
                      type="number" 
                      step="0.01"
                      [(ngModel)]="sec.exercise_price" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Exercise or strike price"
                      [disabled]="sec.security_subtype === 'Preferred Stock' || sec.security_subtype === 'Common Stock'"
                      [placeholder]="isOptionWarrant(sec) ? '0.00' : '—'" />
                  </td>

                  <!-- Original Issue Price (Preferred) -->
                  <td>
                    <input 
                      type="number" 
                      step="0.01"
                      [(ngModel)]="sec.original_issue_price" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Original issue price"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.00' : '—'" />
                  </td>

                  <!-- Conversion Price (Preferred) -->
                  <td>
                    <input 
                      type="number" 
                      step="0.01"
                      [(ngModel)]="sec.conversion_price" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Conversion price"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.00' : '—'" />
                  </td>

                  <!-- Liquidation Multiplier -->
                  <td>
                    <input 
                      type="number" 
                      step="0.1"
                      [(ngModel)]="sec.liquidation_multiplier" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Liquidation preference multiplier"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '1.0' : '—'" />
                  </td>

                  <!-- Participation Rights -->
                  <td>
                    <select 
                      [(ngModel)]="sec.participation" 
                      (change)="onRowChange()"
                      aria-label="Participation rights"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'">
                      <option value="No">No</option>
                      <option value="Yes">Yes</option>
                      <option value="NA">NA</option>
                    </select>
                  </td>

                  <!-- Participation Cap -->
                  <td>
                    <input 
                      type="text" 
                      [(ngModel)]="sec.max_participation_cap" 
                      (change)="onRowChange()"
                      aria-label="Maximum participation cap"
                      [disabled]="sec.security_subtype !== 'Preferred Stock' || sec.participation !== 'Yes'"
                      [placeholder]="sec.participation === 'Yes' ? 'e.g. 3.0x' : '—'" />
                  </td>

                  <!-- Seniority Ranking -->
                  <td>
                    <input 
                      type="number" 
                      [(ngModel)]="sec.seniority" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Seniority ranking tier"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '1' : '—'" />
                  </td>

                  <!-- Issue Date -->
                  <td>
                    <input 
                      type="date" 
                      [(ngModel)]="sec.issue_date" 
                      (change)="onRowChange()"
                      aria-label="Security issue date" />
                  </td>

                  <!-- Annual Dividend Rate % -->
                  <td>
                    <input 
                      type="number" 
                      step="0.1"
                      [(ngModel)]="sec.dividend_rate" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Annual dividend rate percentage"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.0' : '—'" />
                  </td>

                  <!-- Compounding Convention -->
                  <td>
                    <select 
                      [(ngModel)]="sec.compounding_convention" 
                      (change)="onRowChange()"
                      aria-label="Dividend compounding convention"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'">
                      <option value="Annual">Annual</option>
                      <option value="Semi-Annual">Semi-Annual</option>
                      <option value="Quarterly">Quarterly</option>
                      <option value="Monthly">Monthly</option>
                      <option value="Simple">Simple</option>
                      <option value="None">None</option>
                    </select>
                  </td>

                  <!-- Dividends Paid to Date -->
                  <td>
                    <input 
                      type="number" 
                      step="0.01"
                      [(ngModel)]="sec.dividends_paid_to_date" 
                      (change)="onRowChange()"
                      class="text-right"
                      aria-label="Dividends previously paid to date"
                      [disabled]="sec.security_subtype !== 'Preferred Stock'"
                      [placeholder]="sec.security_subtype === 'Preferred Stock' ? '0.00' : '—'" />
                  </td>

                  <!-- Row Delete Action -->
                  <td style="text-align: center;">
                    <button 
                      class="delete-btn" 
                      (click)="state.removeSecurityRow(selectedTable(), $index)" 
                      [disabled]="currentRows().length <= 1"
                      title="Delete security class"
                      aria-label="Delete security class row">
                      ✕
                    </button>
                  </td>
                </tr>
              }

              <!-- Totals Row -->
              <tr class="total-row">
                <td colspan="2"><strong>Total Capital Structure</strong></td>
                <td class="text-right"><strong>{{ totalShares() | number:'1.0-0' }}</strong></td>
                <td colspan="5"></td>
                <td colspan="6"></td>
                <td></td>
              </tr>
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
      align-items: flex-end;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 14px;
    }

    .table-actions-group {
      display: flex;
      gap: 8px;
      align-items: center;
      flex-wrap: wrap;
    }

    .date-selector-tabs {
      display: flex;
      background: #eef3f1;
      padding: 3px;
      border-radius: 6px;
      border: 1px solid #d4dedb;
    }

    .date-selector-tabs button {
      background: transparent;
      border: 0;
      padding: 5px 11px;
      border-radius: 4px;
      font-size: 11.5px;
      font-weight: 600;
      color: #3b4d53;
      cursor: pointer;
      transition: all 0.12s ease;
    }

    .date-selector-tabs button.active {
      background: #ffffff;
      color: var(--teal);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }

    .btn-icon {
      width: 12px;
      height: 12px;
      margin-right: 3px;
    }

    .delete-btn {
      background: transparent;
      border: 0;
      color: #94a3b8;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;
      width: 22px;
      height: 22px;
      line-height: 1;
      border-radius: 4px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: color 0.12s, background 0.12s;
    }

    .delete-btn:hover:not(:disabled) {
      color: var(--danger);
      background: var(--danger-bg);
    }

    .delete-btn:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  `]
})
export class CapTableEditorComponent {
  readonly state = inject(ValuationStateService);
  readonly selectedTable = signal<'calibration' | 'valuation'>('valuation');

  readonly currentRows = computed<SecurityInput[]>(() => {
    const req = this.state.request();
    if (!req) return [];
    return this.selectedTable() === 'calibration' ? req.calibration_securities : req.valuation_securities;
  });

  readonly totalShares = computed(() => {
    return this.currentRows().reduce((acc, s) => acc + (Number(s.shares) || 0), 0);
  });

  isOptionWarrant(sec: SecurityInput): boolean {
    return sec.security_subtype === 'Option' || sec.security_subtype === 'Warrant';
  }

  onSubtypeChange(sec: SecurityInput): void {
    if (sec.security_subtype === 'Common Stock') {
      sec.exercise_price = null;
      sec.original_issue_price = null;
      sec.conversion_price = null;
      sec.liquidation_multiplier = 0.0;
      sec.participation = 'NA';
      sec.max_participation_cap = 'NA';
      sec.seniority = 999;
      sec.dividend_rate = 0.0;
      sec.compounding_convention = 'None';
      sec.dividends_paid_to_date = 0.0;
    } else if (this.isOptionWarrant(sec)) {
      sec.original_issue_price = null;
      sec.conversion_price = null;
      sec.liquidation_multiplier = 0.0;
      sec.participation = 'NA';
      sec.max_participation_cap = 'NA';
      sec.seniority = 999;
      sec.dividend_rate = 0.0;
      sec.compounding_convention = 'None';
      sec.dividends_paid_to_date = 0.0;
      if (sec.exercise_price === null) sec.exercise_price = 100.0;
    } else if (sec.security_subtype === 'Preferred Stock') {
      if (!sec.liquidation_multiplier) sec.liquidation_multiplier = 1.0;
      if (!sec.seniority || sec.seniority === 999) sec.seniority = 1;
      if (!sec.participation || sec.participation === 'NA') sec.participation = 'No';
      if (!sec.max_participation_cap) sec.max_participation_cap = 'NA';
      if (!sec.compounding_convention || sec.compounding_convention === 'None') sec.compounding_convention = 'Annual';
    }
    this.onRowChange();
  }

  private previousSecurityName: string = '';

  onSecurityNameFocus(name: string): void {
    this.previousSecurityName = (name || '').trim();
  }

  onSecurityNameChange(sec: SecurityInput): void {
    const newName = (sec.security || '').trim();
    if (this.selectedTable() === 'calibration') {
      const currentCalSec = this.state.request()?.calibration_security_name?.trim();
      if (this.previousSecurityName && currentCalSec === this.previousSecurityName && newName) {
        this.state.request.update(r => r ? { ...r, calibration_security_name: newName } : null);
      }
    }
    if (this.selectedTable() === 'valuation' && this.previousSecurityName && newName) {
      this.state.request.update(r => {
        if (!r) return null;
        const updatedHoldings = r.holdings.map(h => h.security === this.previousSecurityName ? { ...h, security: newName } : h);
        return { ...r, holdings: updatedHoldings };
      });
    }
    this.previousSecurityName = newName;
    this.onRowChange();
  }

  onRowChange(): void {
    this.state.ensureValidCalibrationSecurity();
    this.state.calculate();
  }
}
