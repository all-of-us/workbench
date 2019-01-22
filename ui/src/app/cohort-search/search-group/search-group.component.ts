import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {List} from 'immutable';
import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {CohortSearchActions, CohortSearchState, groupError} from '../redux';

import {CohortStatus, SearchRequest, TemporalMention, TemporalTime} from 'generated';
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
  temporalFlag = false;
  whichMention = [this.formatStatusForText(TemporalMention.ANYMENTION),
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION];
  timeDropDown = [TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF,
    ];
  dropdownOption: any;
  timeDropdownOption: any;
  subscription: Subscription;

  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => this.error = error);
      this.temporalFlag = this.group.get('temporal');
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
    e.target.checked === true ?
      this.temporalFlag = true : this.temporalFlag = false;
  }

  getMentionTitle(mention) {
    // this.formatStatusForText(mention);
    this.dropdownOption = mention;

  }
  getTimeTitle(time) {
    this.timeDropdownOption = time;
  }

 formatStatusForText(mention: TemporalMention): string {
    return {
      [TemporalMention.ANYMENTION]: 'Any Mention',
      [TemporalMention.FIRSTMENTION]: 'Any Mention',
      [TemporalMention.LASTMENTION]: 'Any Mention',
    }[mention];
  }
}
