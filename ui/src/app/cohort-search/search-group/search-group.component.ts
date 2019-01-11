import {NgRedux} from '@angular-redux/store';
import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
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
  @Input() role: keyof SearchRequest;

  error: boolean;
  status: string;
  undoTimer: any;
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

  remove() {
    const groupCard = document.getElementById(this.group.get('id'));
    const {marginBottom, width, height} = window.getComputedStyle(groupCard);
    const overlay = document.getElementById('overlay_' + this.group.get('id'));
    const styles = 'width:' + width + '; height:' + height + '; margin: -'
      + (parseFloat(height) + parseFloat(marginBottom)) + 'px 0 ' + marginBottom + ';';
    overlay.setAttribute('style', styles);
    this.status = 'pending';
    this.undoTimer = setTimeout(() => {
      this.actions.removeGroup(this.role, this.groupId);
      this.status = 'deleted';
    }, 3000);
  }

  undo() {
    clearTimeout(this.undoTimer);
    this.status = 'active';
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
}
