import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {CohortSearchActions, CohortSearchState, getItem, groupError} from 'app/cohort-search/redux';
import {SearchRequest, TemporalMention, TemporalTime} from 'generated';
import {List, Map} from 'immutable';
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
  whichMention = [TemporalMention.ANYMENTION,
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION];
  timeDropDown = [TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF];
  subscription: Subscription;
  itemSubscription: Subscription;
  private item: Map<any, any> = Map();
  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  tempGroup: any;

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => {
        this.error = error;
      });
    this.itemSubscription = this.ngRedux.select(getItem(this.group.get('id')))
      .subscribe(item => this.item = item);

  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get isRequesting() {
    return this.group.get('isRequesting', false);
  }

  get temporalFlag() {
    // console.log(this.group.get('temporal'));
    return this.group.get('temporal');
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

  get mention() {
    return this.group.get('mention');
  }

  get time() {
    return this.group.get('time');
  }

  get timeValue() {
    return this.group.get('timeValue');
  }

  getTemporalGroup(e) {
    this.tempGroup = e;
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
    this.actions.updateTemporal(e.target.checked, this.groupId);
  }

  getMentionTitle(mention) {
    this.actions.updateWhichMention(mention, this.groupId);
  }

  getTimeTitle(time) {
    this.actions.updateTemporalTime(time, this.groupId);
  }

  getTimeValue(e) {
    this.actions.updateTemporalTimeValue(e.target.value, this.groupId);
  }

  formatStatusForText(mention: TemporalMention): string {
    return {
      [TemporalMention.ANYMENTION]: 'Any Mention',
      [TemporalMention.FIRSTMENTION]: 'First Mention',
      [TemporalMention.LASTMENTION]: 'Last Mention',
    }[mention];
  }

  formatStatus(options) {
    switch (options) {
      case 'ANY_MENTION' :
        return 'Any Mention';
      case 'FIRST_MENTION' :
        return 'First Mention';
      case 'LAST_MENTION' :
        return 'Last Mention';
      case 'DURING_SAME_ENCOUNTER_AS' :
        return 'During same encounter as';
      case 'X_DAYS_BEFORE' :
        return 'X Days before';
      case 'X_DAYS_AFTER' :
        return 'X Days after';
      case 'WITHIN_X_DAYS_OF' :
        return 'Within X Days of';
    }
  }
}
