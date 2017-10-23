import {Component, Input, EventEmitter, Output} from '@angular/core';

import {CohortSearchActions} from '../redux';
import {SearchRequest} from 'generated';

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
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
})
export class SearchGroupComponent {
  @Input() group;
  @Input() role: keyof SearchRequest;
  @Output() onRemove = new EventEmitter<boolean>();

  readonly criteriaTypes = CRITERIA_TYPES;

  constructor(private actions: CohortSearchActions) {}

  get isRequesting() {
    return this.group.get('isRequesting', false);
  }

  get groupId() {
    return this.group.get('id');
  }

  remove(event) {
    this.onRemove.emit(true);
  }

  launchWizard(criteriaType: string) {
    this.actions.setActiveContext({
      criteriaType,
      role: this.role,
      groupId: this.groupId
    });

    this.actions.setWizardOpen();
    this.actions.initGroupItem(this.groupId);
  }
}
