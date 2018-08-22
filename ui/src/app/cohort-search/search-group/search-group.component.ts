import {Component, Input} from '@angular/core';
import {List} from 'immutable';

import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {CohortSearchActions} from '../redux';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group',
  templateUrl: './search-group.component.html',
  styleUrls: [
    './search-group.component.css',
    '../../styles/buttons.css',
  ]
})
export class SearchGroupComponent {
  @Input() group;
  @Input() role: keyof SearchRequest;

  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;

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

  launchWizard(criteria: any) {
    // if(criteria.name === 'Conditions'){
    //     criteria.name = 'ICD9 Codes'
    //     const criteriaType = 'ICD9';
    //     const itemId = this.actions.generateId('items');
    //     const fullTree = criteria.fullTree || false;
    //     const {role, groupId} = this;
    //     const context = {criteriaType, role, groupId, itemId, fullTree};
    //     this.actions.openWizard(itemId, context);
    // } else{
        const itemId = this.actions.generateId('items');
        const criteriaType = criteria.type;
        const fullTree = criteria.fullTree || false;
        const {role, groupId} = this;
        const context = {criteriaType, role, groupId, itemId, fullTree};
        this.actions.openWizard(itemId, context);
    // }

  }
}
