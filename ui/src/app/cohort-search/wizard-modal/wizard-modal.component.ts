import {
  Component,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Wizard} from 'clarity-angular';
import {Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
  activeCriteriaList,
  activeRole,
  activeGroupId,
  activeItem,
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
    const state = this.ngRedux.getState();
    const role = activeRole(state);
    const groupId = activeGroupId(state);
    const itemId = activeItem(state).get('id');
    const selections = activeCriteriaList(state);

    this.actions.finishWizard();
    if (!selections.isEmpty()) {
      this.actions.requestItemCount(role, itemId);
      this.actions.requestGroupCount(role, groupId);
      this.actions.requestTotalCount();
    }
  }
}
