import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {
  CohortSearchActions,
  CohortSearchState,
  getTemporalGroupItems,
  groupError
} from 'app/cohort-search/redux';
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
  // @select (getTemporalGroupItems) getTemporalGroupItems$;
  nonTemporalItems = [];
  temporalItems = [];
  error: boolean;
  dropdownOption = {
    selected: [TemporalMention.ANYMENTION, TemporalMention.ANYMENTION, TemporalMention.ANYMENTION]
  };

  whichMention = [TemporalMention.ANYMENTION,
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION];

  timeDropDown = [TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF];

  subscription: Subscription;
  itemSubscription: Subscription;
  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  tempGroup: any;
  private item: Map<any, any> = Map();
  itemId: any;

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => {
        this.error = error;
      });

    this.itemSubscription = this.ngRedux.select(getTemporalGroupItems(this.group.get('id')))
      .filter(temporal => !!temporal)
      .subscribe(i => {
        this.nonTemporalItems = i.nonTemporalItems;
        this.temporalItems = i.temporalItems;
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    this.itemSubscription.unsubscribe();
  }


  get isRequesting() {
    return this.group.get('isRequesting', false);
  }

  get temporalFlag() {
    return this.group.get('temporal');
  }

  get groupId() {
    return this.group.get('id');
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
    // console.log(this.tempGroup);
  }

  get items() {
    return this.group.get('items', List());
  }

  launchWizard(criteria: any, tempGroup?: number) {
    const itemId = this.actions.generateId('items');
    const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
    const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : null;
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    const {role, groupId} = this;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    this.actions.openWizard(itemId, criteria.type, context, tempGroup);
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
