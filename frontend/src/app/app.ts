import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValuationStateService } from './services/valuation-state.service';
import { HeaderComponent } from './components/header/header.component';
import { EngagementSettingsComponent } from './components/engagement-settings/engagement-settings.component';
import { CapTableEditorComponent } from './components/cap-table-editor/cap-table-editor.component';
import { OpmControlsComponent } from './components/opm-controls/opm-controls.component';
import { ClientHoldingsEditorComponent } from './components/client-holdings-editor/client-holdings-editor.component';
import { CapitalIqModalComponent } from './components/capital-iq-modal/capital-iq-modal.component';
import { ReportExhibitsComponent } from './components/report-exhibits/report-exhibits.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent,
    EngagementSettingsComponent,
    CapTableEditorComponent,
    OpmControlsComponent,
    ClientHoldingsEditorComponent,
    CapitalIqModalComponent,
    ReportExhibitsComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  readonly state = inject(ValuationStateService);
  readonly validationDismissed = signal(false);

  dismissValidation(): void {
    this.validationDismissed.set(true);
  }

  showValidation(): void {
    this.validationDismissed.set(false);
  }
}
