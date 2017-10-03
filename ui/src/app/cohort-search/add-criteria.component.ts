import {Component, Input} from '@angular/core';

import {CohortSearchActions} from './actions';
import {SearchGroupRole} from './store.interfaces';


const CRITERIA_TYPES = [
  { id: 1, name: 'Demographics', type: 'demo' },
  { id: 2, name: 'ICD9 Codes', type: 'icd9' },
  { id: 3, name: 'ICD10 Codes', type: 'icd10' },
  { id: 4, name: 'PheCodes', type: 'phecodes' },
  { id: 5, name: 'CPT Codes', type: 'cpt' },
  { id: 6, name: 'Medications', type: 'meds' },
  { id: 7, name: 'Labs', type: 'labs' },
  { id: 8, name: 'Vitals', type: 'vitals' },
  { id: 8, name: 'Temporal', type: 'temporal' }
];


@Component({
  selector: 'app-add-criteria',
  template: `
    <clr-dropdown>
      <button type="button" class="btn btn-link" clrDropdownTrigger>
        Add Criteria <clr-icon shape="caret down"></clr-icon>
      </button>
      <clr-dropdown-menu clrPosition="bottom-left" *clrIfOpen>
        <button
          class="dropdown-item"
          *ngFor="let criteria of criteriaTypes"
          type="button"
          (click)="actions.openWizard(criteria.type, index, role)"
          clrDropdownItem>{{criteria.name}}</button>
      </clr-dropdown-menu>
    </clr-dropdown>
  `
})
export class AddCriteriaComponent {
  @Input() index: number;
  @Input() role: SearchGroupRole;

  readonly criteriaTypes = CRITERIA_TYPES;
  constructor(private actions: CohortSearchActions) {}
}
