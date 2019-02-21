import {NgRedux} from '@angular-redux/store';
import {AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {
  CohortSearchActions,
  CohortSearchState,
  getTemporalGroupItems,
  groupError
} from 'app/cohort-search/redux';
import {integerAndRangeValidator} from 'app/cohort-search/validators';
import {SearchRequest, TemporalMention, TemporalTime, TreeType} from 'generated';
import {List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-search-group',
  templateUrl: './search-group.component.html',
  styleUrls: [
    './search-group.component.css',
    '../../styles/buttons.css',
  ]
})
export class SearchGroupComponent implements AfterViewInit, OnInit, OnDestroy {
  @Input() group;
  @Input() index;
  @Input() role: keyof SearchRequest;
  @Output() temporalLength = new EventEmitter<any>();
  nonTemporalItems = [];
  temporalItems = [];
  error: boolean;
  whichMention = [TemporalMention.ANYMENTION,
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION];

  timeDropDown = [TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF];
  name = this.whichMention[0];
  subscription: Subscription;
  itemSubscription: Subscription;
  readonly domainTypes = DOMAIN_TYPES;
  readonly programTypes = PROGRAM_TYPES;
  itemId: any;
  treeType = [];
  timeForm = new FormGroup({
    inputTimeValue: new FormControl([Validators.required],
      [integerAndRangeValidator('Form', 0, 9999)]),
  });

  constructor(private actions: CohortSearchActions, private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(groupError(this.group.get('id')))
      .subscribe(error => {
        this.error = error;
      });

    this.itemSubscription = this.ngRedux.select(getTemporalGroupItems(this.group.get('id')))
      .filter(temporal => !!temporal)
      .subscribe(i => {
        this.treeType = i.type;
        this.nonTemporalItems = i.nonTemporalItems;
        this.temporalItems = i.temporalItems;
        this.temporalLength.emit( {
          tempLength: this.warningFlag,
          flag: this.temporalFlag}
          );
      });
  }

  ngAfterViewInit() {
    if (typeof ResizeObserver === 'function') {
      const ro = new ResizeObserver(() => {
        if (this.status === 'hidden' || this.status === 'pending') {
          this.setOverlayPosition();
        }
      });
      const groupDiv = document.getElementById(this.group.get('id'));
      ro.observe(groupDiv);
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    this.itemSubscription.unsubscribe();
  }

  get typeFlag() {
    let flag = true;
    this.treeType.map(m => {
      if ( m === TreeType[TreeType.PM] || m === TreeType[TreeType.DEMO] ||
        m === TreeType[TreeType.PPI]) {
        flag = false;
      }
    });
    return flag;
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

  get status() {
    return this.group.get('status');
  }

  get items() {
    return this.group.get('items', List());
  }

  remove() {
    this.hide('pending');
    const timeoutId = setTimeout(() => {
      this.actions.removeGroup(this.role, this.groupId);
    }, 10000);
    // For some reason Angular will delete the timeout id from scope if the inputs change, so we
    // have to keep in the redux store
    this.actions.setTimeoutId('groups', this.groupId, timeoutId);
  }

  hide(status: string) {
    setTimeout(() => this.setOverlayPosition());
    this.actions.removeGroup(this.role, this.groupId, status);
  }

  enable() {
    this.actions.enableGroup(this.group);
  }

  undo() {
    clearTimeout(this.group.get('timeout'));
    this.enable();
  }

  setOverlayPosition() {
    const groupCard = document.getElementById(this.group.get('id'));
    if (groupCard) {
      const {marginBottom, width, height} = window.getComputedStyle(groupCard);
      const overlay = document.getElementById('overlay_' + this.group.get('id'));
      const styles = 'width:' + width + '; height:' + height + '; margin: -'
        + (parseFloat(height) + parseFloat(marginBottom)) + 'px 0 ' + marginBottom + ';';
      overlay.setAttribute('style', styles);
    }
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

  launchWizard(criteria: any, tempGroup?: number) {
    const itemId = this.actions.generateId('items');
    const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
    const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : criteria.subtype;
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    const {role, groupId} = this;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    this.actions.openWizard(itemId, criteria.type, context, tempGroup);
  }

  getTemporal(e) {
    this.actions.updateTemporal(e.target.checked, this.groupId, this.role);
  }

  getMentionTitle(mentionName) {
    this.actions.updateWhichMention(mentionName, this.groupId, this.role);
  }

  getTimeTitle(timeName) {
    this.actions.updateTemporalTime(timeName, this.groupId, this.role );
  }

  getTimeValue(e) {
    if (e.target.value >= 0) {
      this.actions.updateTemporalTimeValue(e.target.value, this.groupId, this.role);
    }
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

  get groupDiasbleFlag() {
    const itemLength = this.group.get('items', List).toJS().length;
    if (!this.temporalFlag && !itemLength ) {
      return true;
    } else {
      return false;
    }
  }

  get warningFlag() {
    const tempWarningFlag =
      !this.temporalItems.filter(t => t.get('status') === 'active').length;
    const nonTempWarningFlag =
      !this.nonTemporalItems.filter(t => t.get('status') === 'active').length;
    return tempWarningFlag || nonTempWarningFlag;
  }
}
