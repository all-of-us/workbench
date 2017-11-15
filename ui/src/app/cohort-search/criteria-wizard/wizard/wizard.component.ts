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
    this.actions.finishWizard();
  }
}
