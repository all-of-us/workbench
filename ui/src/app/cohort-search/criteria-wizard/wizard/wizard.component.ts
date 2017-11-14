import {Component, Input, ViewEncapsulation} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {
  CohortSearchActions,
  CohortSearchState,
  activeParameterList,
  activeRole,
  activeGroupId,
  activeItem,
} from '../../redux';


@Component({
  selector: 'app-criteria-wizard',
  templateUrl: './wizard.component.html',
  styleUrls: ['./wizard.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class WizardComponent {
  @Input() open: boolean;
  @Input() criteriaType: string;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  get critPageTitle() {
    let _type = this.criteriaType;
    if (_type.match(/^DEMO.*/i)) {
      _type = 'Demographics';
    } else if (_type.match(/^(ICD|CPT).*/i)) {
      _type = _type.toUpperCase();
    }
    return `Choose ${_type} Codes`;
  }

  cancel() {
    this.actions.cancelWizard();
  }

  finish() {
    const state = this.ngRedux.getState();
    const role = activeRole(state);
    const groupId = activeGroupId(state);
    const itemId = activeItem(state).get('id');
    const selections = activeParameterList(state);
    this.actions.finishWizard();

    if (!selections.isEmpty()) {
      this.actions.requestItemCount(role, itemId);
      this.actions.requestGroupCount(role, groupId);
      this.actions.requestTotalCount(groupId);
    }
  }
}
