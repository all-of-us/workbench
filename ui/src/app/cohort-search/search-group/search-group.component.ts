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

  // ******* Temporal Changes are commented bellow Please
  // uncomment below codes when using Temporal HTML ********** //

  // temporalDropdown = false;
  // whichMention = ['Any mention', 'First mention', 'Last mention'];
  // timeDropDown = ['During same encounter as',
  //                 'X Days before', 'X Days after', 'Within X days of',
  //   'On or X days before', 'On or X days after'];
  // dropdownOption: any;
  // timeDropdownOption: any;

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
    const itemId = this.actions.generateId('items');
    const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
    const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : null;
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    const {role, groupId} = this;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    this.actions.openWizard(itemId, criteria.type, context);
  }
  // *******Temporal Changes are commented bellow Please
  // uncomment below codes when using Temporal HTML ****** //

  // getTemporal(e) {
  //   if (e.target.checked === true) {
  //     this.temporalDropdown = true;
  //   } else {
  //     this.temporalDropdown = false;
  //   }
  //
  // }
  //
  // getMentionTitle(mention) {
  //   this.dropdownOption = mention;
  // }
  // getTimeTitle(time) {
  //   this.timeDropdownOption = time;
  // }
}
