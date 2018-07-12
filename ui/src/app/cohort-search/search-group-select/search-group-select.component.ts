import {Component, Input} from '@angular/core';

import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {CohortSearchActions} from '../redux';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-select',
  templateUrl: './search-group-select.component.html',
  styleUrls: ['./search-group-select.component.css']
})
export class SearchGroupSelectComponent {
  @Input() role: keyof SearchRequest;

  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;

  constructor(private actions: CohortSearchActions) {}

  launchWizard(criteriaType: string) {
    const itemId = this.actions.generateId('items');
    const groupId = this.actions.generateId(this.role);
    this.actions.initGroup(this.role, groupId);
    const role = this.role;
    const context = {criteriaType, role, groupId, itemId};
    this.actions.openWizard(itemId, context);
  }
}
