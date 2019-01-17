import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {environment} from 'environments/environment';
import {List} from 'immutable';
import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {CohortSearchActions, CohortSearchState, groupError} from '../redux';

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

  get status() {
    return this.group.get('status');
  }

  get items() {
    return this.group.get('items', List());
  }

  remove() {
    this.hide('pending');
    this.undoTimer = setTimeout(() => {
      this.actions.removeGroup(this.role, this.groupId);
    }, 3000);
  }

  hide(status: string) {
    setTimeout(() => this.setOverlayPosition());
    this.actions.removeGroup(this.role, this.groupId, status);
  }

  enable() {
    this.actions.enableGroup(this.group);
  }

  undo() {
    // For some reason Angular clears the timeout id from 'this' when the inputs change, so we'll
    // basically clear by brute-force until we can find a better solution
    for (let i = 1; i < 99999; i++) {
      clearTimeout(i);
    }
    this.enable();
  }

  setOverlayPosition() {
    const groupCard = document.getElementById(this.group.get('id'));
    const {marginBottom, width, height} = window.getComputedStyle(groupCard);
    const overlay = document.getElementById('overlay_' + this.group.get('id'));
    const styles = 'width:' + width + '; height:' + height + '; margin: -'
      + (parseFloat(height) + parseFloat(marginBottom)) + 'px 0 ' + marginBottom + ';';
    overlay.setAttribute('style', styles);
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
