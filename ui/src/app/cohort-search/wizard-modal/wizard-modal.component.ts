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

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent {

  @select(['context', 'wizardOpen']) readonly open$: Observable<boolean>;
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
