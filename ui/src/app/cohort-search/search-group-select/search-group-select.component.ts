import {Component, Input} from '@angular/core';

import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {CohortSearchActions} from 'app/cohort-search/redux';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-select',
  templateUrl: './search-group-select.component.html',
  styleUrls: ['./search-group-select.component.css']
})
export class SearchGroupSelectComponent {
  @Input() role: keyof SearchRequest;
  @Input() index: number;

  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  position = 'bottom-left';

  constructor(private actions: CohortSearchActions) {}

  launchWizard(criteria: any) {
    const itemId = this.actions.generateId('items');
    const groupId = this.actions.generateId(this.role);
    const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
    const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : null;
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    this.actions.initGroup(this.role, groupId);
    const role = this.role;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    this.actions.openWizard(itemId, criteria.type, context);
  }

  setMenuPosition() {
    const dropdown = document.getElementById(this.role + '-button').getBoundingClientRect();
    this.position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
  }
}
