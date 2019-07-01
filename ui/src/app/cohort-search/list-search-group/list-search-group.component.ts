import {AfterViewInit, Component, Input} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {LIST_DOMAIN_TYPES, LIST_PROGRAM_TYPES} from 'app/cohort-search/constant';
import {searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {generateId, mapGroup} from 'app/cohort-search/utils';
import {integerAndRangeValidator} from 'app/cohort-search/validators';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType, SearchRequest, TemporalMention, TemporalTime} from 'generated/fetch';

@Component({
  selector: 'app-list-search-group',
  templateUrl: './list-search-group.component.html',
  styleUrls: [
    './list-search-group.component.css',
    '../../styles/buttons.css',
  ]
})
export class ListSearchGroupComponent implements AfterViewInit {
  @Input() group;
  @Input() index;
  @Input() role: keyof SearchRequest;
  @Input() updateRequest: Function;

  whichMention = [
    TemporalMention.ANYMENTION,
    TemporalMention.FIRSTMENTION,
    TemporalMention.LASTMENTION
  ];

  timeDropDown = [
    TemporalTime.DURINGSAMEENCOUNTERAS,
    TemporalTime.XDAYSAFTER,
    TemporalTime.XDAYSBEFORE,
    TemporalTime.WITHINXDAYSOF
  ];
  name = this.whichMention[0];
  readonly domainTypes = LIST_DOMAIN_TYPES;
  readonly programTypes = LIST_PROGRAM_TYPES;
  itemId: any;
  treeType = [];
  timeForm = new FormGroup({
    inputTimeValue: new FormControl([Validators.required],
      [integerAndRangeValidator('Form', 0, 9999)]),
  });
  demoOpen = false;
  demoMenuHover = false;
  position = 'bottom-left';
  count: number;
  error = false;
  loading = false;

  ngAfterViewInit() {
    if (typeof ResizeObserver === 'function') {
      const ro = new ResizeObserver(() => {
        if (this.status === 'hidden' || this.status === 'pending') {
          this.setOverlayPosition();
        }
      });
      const groupDiv = document.getElementById(this.group.id);
      ro.observe(groupDiv);
    }
    const demoItem = document.getElementById('DEMO-' + this.index);
    if (demoItem) {
      demoItem.addEventListener('mouseenter', () => {
        this.demoOpen = true;
        setTimeout(() => {
          const demoMenu = document.getElementById('demo-menu-' + this.index);
          demoMenu.addEventListener('mouseenter', () => this.demoMenuHover = true);
          demoMenu.addEventListener('mouseleave', () => this.demoMenuHover = false);
        });
      });
      demoItem.addEventListener('mouseleave', () => this.demoOpen = false);
    }
  }

  getGroupCount() {
    try {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const group = mapGroup(this.group);
      const request = <SearchRequest>{
        includes: [],
        excludes: [],
        [this.role]: [group]
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(count => {
        this.count = count;
        this.loading = false;
      }, (err) => {
        console.error(err);
        this.error = true;
        this.loading = false;
      });
    } catch (error) {
      console.error(error);
      this.error = true;
      this.loading = false;
    }
  }

  update = () => {
    // timeout prevents Angular 'value changed after checked' error
    setTimeout(() => {
      if (this.activeItems) {
        this.updateRequest();
        this.loading = true;
        this.error = false;
        this.getGroupCount();
      }
    });
  }

  get activeItems() {
    return this.group.items.some(it => it.status === 'active');
  }

  get items() {
    return !this.group.temporal ? this.group.items
      : this.group.items.filter(it => it.temporalGroup === 0);
  }

  get temporalItems() {
    return !this.group.temporal ? [] : this.group.items.filter(it => it.temporalGroup === 1);
  }

  get disableTemporal() {
    return this.items.some(it =>
      [DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(it.type));
  }

  get temporal() {
    return this.group.temporal;
  }

  get groupId() {
    return this.group.id;
  }

  get status() {
    return this.group.status;
  }

  remove() {
    this.hide('pending');
    const timeoutId = setTimeout(() => {
      this.removeGroup();
    }, 10000);
    this.setGroupProperty('timeout', timeoutId);
  }

  hide(status: string) {
    setTimeout(() => this.setOverlayPosition());
    this.removeGroup(status);
  }

  enable() {
    this.setGroupProperty('status', 'active');
  }

  undo() {
    clearTimeout(this.group.timeout);
    this.enable();
  }

  removeGroup(status?: string): void {
    const searchRequest = searchRequestStore.getValue();
    if (!status) {
      searchRequest[this.role] = searchRequest[this.role].filter(grp => grp.id !== this.group.id);
      searchRequestStore.next(searchRequest);
    } else {
      this.setGroupProperty('status', status);
    }
  }

  setOverlayPosition() {
    const groupCard = document.getElementById(this.group.id);
    if (groupCard) {
      const {marginBottom, width, height} = window.getComputedStyle(groupCard);
      const overlay = document.getElementById('overlay_' + this.group.id);
      const styles = 'width:' + width + '; height:' + height + '; margin: -'
        + (parseFloat(height) + parseFloat(marginBottom)) + 'px 0 ' + marginBottom + ';';
      overlay.setAttribute('style', styles);
    }
  }

  get mention() {
    return this.group.mention;
  }

  get time() {
    return this.group.time;
  }

  get timeValue() {
    return this.group.timeValue;
  }

  launchWizard(criteria: any, tempGroup?: number) {
    const {domain, type, standard} = criteria;
    const itemId = generateId('items');
    tempGroup = tempGroup || 0;
    const item = this.initItem(itemId, domain, tempGroup);
    const codes = criteria.codes || false;
    const role = this.role;
    const groupId = this.group.id;
    const context = {item, domain, type, standard, role, groupId, itemId, codes, tempGroup};
    wizardStore.next(context);
  }

  initItem(id: string, type: string, tempGroup: number) {
    return {
      id,
      type,
      searchParameters: [],
      modifiers: [],
      count: null,
      temporalGroup: tempGroup,
      status: 'active'
    };
  }

  setGroupProperty(property: string, value: any) {
    const searchRequest = searchRequestStore.getValue();
    searchRequest[this.role] = searchRequest[this.role].map(grp => {
      if (grp.id === this.group.id) {
        grp[property] = value;
      }
      return grp;
    });
    searchRequestStore.next(searchRequest);
    this.updateRequest();
  }

  getTemporal(e) {
    this.setGroupProperty('temporal', e.target.checked);
    if ((!e.target.checked && this.activeItems) || (e.target.checked && !this.temporalError)) {
      this.loading = true;
      this.error = false;
      this.getGroupCount();
    }
  }

  getMentionTitle(mentionName) {
    this.setGroupProperty('mention', mentionName);
    // TODO when new api call are ready, recalculate counts
  }

  getTimeTitle(timeName) {
    this.setGroupProperty('time', timeName);
    // TODO when new api call are ready, recalculate counts
  }

  getTimeValue(e) {
    if (e.target.value >= 0) {
      this.setGroupProperty('timeValue', e.target.value);
      // TODO when new api call are ready, recalculate counts
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

  get groupDisableFlag() {
    return !this.temporal && !this.group.items.length;
  }

  get temporalError() {
    const counts = this.group.items.reduce((acc, it) => {
      if (it.status === 'active') {
        acc[it.temporalGroup]++;
      }
      return acc;
    }, [0, 0]);
    return counts.includes(0);
  }

  setMenuPosition() {
    const id = this.role + this.index + '-button';
    const dropdown = document.getElementById(id).getBoundingClientRect();
    this.position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
  }
}
