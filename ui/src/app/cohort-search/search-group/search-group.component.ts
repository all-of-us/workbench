import {Component, Input} from '@angular/core';
import {List} from 'immutable';

import {CRITERIA_TYPES} from '../constant';
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
