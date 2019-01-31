import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {CohortSearchActions, CohortSearchState, groupError} from 'app/cohort-search/redux';
import {environment} from 'environments/environment';
import {List} from 'immutable';

import {SearchRequest} from 'generated';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-search-group',
  templateUrl: './search-group.component.html',
  styleUrls: [
    './search-group.component.css',
    '../../styles/buttons.css',
  ]
})
export class SearchGroupComponent implements OnInit, OnDestroy {
  @Input() group;
  @Input() index;
  @Input() role: keyof SearchRequest;

  error: boolean;
  temporalDropdown = false;
  whichMention = ['Any mention', 'First mention', 'Last mention'];
  timeDropDown = ['During same encounter as',
    'X Days before', 'X Days after', 'Within X days of',
    'On or X days before', 'On or X days after'];
  dropdownOption: any;
  timeDropdownOption: any;
  subscription: Subscription;

  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  readonly envFlag = environment.enableTemporal;
  position = 'bottom-left';

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => this.error = error);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

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

  getTemporal(e) {
    if (e.target.checked === true) {
      this.temporalDropdown = true;
    } else {
      this.temporalDropdown = false;
    }

  }

  getMentionTitle(mention) {
    this.dropdownOption = mention;
  }
  getTimeTitle(time) {
    this.timeDropdownOption = time;
  }

  setMenuPosition() {
    const id = this.role + this.index + '-button';
    const dropdown = document.getElementById(id).getBoundingClientRect();
    this.position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
  }
}
