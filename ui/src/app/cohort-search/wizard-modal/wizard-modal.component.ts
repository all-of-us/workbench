import {
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Wizard} from 'clarity-angular';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
  wizardOpen,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent {

  @select(wizardOpen) readonly open$: Observable<boolean>;
  @select(activeCriteriaType) readonly critType$: Observable<string>;
  @ViewChild('wizard') wizard: Wizard;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  close() {
    this.actions.cancelWizard();
  }

  finish() {
    this.actions.finishWizard();
  }
}
