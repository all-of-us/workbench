import {
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Wizard} from 'clarity-angular/wizard/wizard';

import {CohortSearchActions} from '../actions';
import {CohortSearchState, wizardOpen, activeCriteriaType} from '../store';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent {

  @select(wizardOpen) readonly open$: Observable<boolean>;
  @select(activeCriteriaType) critType$: Observable<string>;
  @ViewChild('wizard') wizard: Wizard;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  close() {
    this.wizard.close();
    this.actions.cancelWizard();
  }

  finish() {
    this.wizard.finish();
    this.actions.finishWizard();
  }
}
