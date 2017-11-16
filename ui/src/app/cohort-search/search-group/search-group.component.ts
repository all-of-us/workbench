import {Component, Input} from '@angular/core';
import {List} from 'immutable';

import {CohortSearchActions} from '../redux';

import {SearchRequest} from 'generated';

const CRITERIA_TYPES = [
  { name: 'Demographics', type: 'demo' },
  { name: 'ICD9 Codes',   type: 'icd9' },
  { name: 'ICD10 Codes',  type: 'icd10' },
  { name: 'PheCodes',     type: 'phecode' },
  { name: 'CPT Codes',    type: 'cpt' },
  { name: 'Medications',  type: 'meds' },
  { name: 'Labs',         type: 'labs' },
  { name: 'Vitals',       type: 'vitals' },
  { name: 'Temporal',     type: 'temporal' }
];

@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
})
export class SearchGroupComponent {
  @Input() group;
  @Input() role: keyof SearchRequest;

  readonly criteriaTypes = CRITERIA_TYPES;

  constructor(private actions: CohortSearchActions) {}

  get isRequesting() {
    return this.group.get('isRequesting', false);
  }

  get groupId() {
    return this.group.get('id');
  }

  get items() {
    return this.group.get('items', List());
  }

  remove(event) {
    this.actions.removeGroup(this.role, this.groupId);
  }

  launchWizard(criteriaType: string) {
    const itemId = this.actions.generateId('items');
    const {role, groupId} = this;
    const context = {criteriaType, role, groupId, itemId};
    this.actions.openWizard(itemId, context);
  }
}
