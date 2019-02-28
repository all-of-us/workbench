import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';

import {SearchRequest, TreeType} from 'generated';
import {List, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions, CohortSearchState, getItem, itemError, parameterList} from 'app/cohort-search/redux';

import {attributeDisplay, getCodeOptions, nameDisplay, typeDisplay, typeToTitle, } from 'app/cohort-search/utils';

@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
  styleUrls: ['./search-group-item.component.css'],
})
export class SearchGroupItemComponent implements OnInit, OnDestroy {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() itemId: string;

  error: boolean;
  private item: Map<any, any> = Map();
  private rawCodes: List<any> = List();
  private subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(getItem(this.itemId))
      .subscribe(item => this.item = item);

    this.subscription.add(this.ngRedux.select(parameterList(this.itemId))
      .subscribe(rawCodes => this.rawCodes = rawCodes));

    this.subscription.add(this.ngRedux.select(itemError(this.itemId))
      .subscribe(error => this.error = error));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get codeType() {
    return typeToTitle(this.item.get('type', ''));
  }

  get codeTypeDisplay() {
    return `${this.codeType} ${this.pluralizedCode}`;
  }

  get pluralizedCode() {
    return this.rawCodes.count() > 1 ? 'Codes' : 'Code';
  }

  get isRequesting() {
    return this.item.get('isRequesting', false);
  }

  get status() {
    return this.item.get('status');
  }

  get codes() {
    const _type = this.item.get('type', '');
    const formatter = (param) => {
      let funcs = [typeDisplay, attributeDisplay];
      if (_type === TreeType[TreeType.DEMO]) {
        funcs = [typeDisplay, nameDisplay, attributeDisplay];
      } else if (_type === TreeType[TreeType.PM]
        || _type === TreeType[TreeType.VISIT]
        || _type === TreeType[TreeType.DRUG]
        || _type === TreeType[TreeType.MEAS]
        || _type === TreeType[TreeType.PPI]) {
        funcs = [nameDisplay];
      }
      return funcs.map(f => f(param)).join(' ').trim();
    };
    const sep = _type === TreeType[TreeType.DEMO] ? '; ' : ', ';
    return this.rawCodes.map(formatter).join(sep);
  }

  remove() {
    this.hide('pending');
    const timeoutId = setTimeout(() => {
      this.actions.removeGroupItem(this.role, this.groupId, this.itemId);
    }, 10000);
    // For some reason Angular will delete the timeout id from scope if the inputs change, so we
    // have to keep in the redux store
    this.actions.setTimeoutId('items', this.itemId, timeoutId);
  }

  hide(status: string) {
    this.actions.removeGroupItem(this.role, this.groupId, this.itemId, status);
  }

  enable() {
    this.actions.enableGroupItem(this.role, this.groupId, this.itemId);
  }

  undo() {
    clearTimeout(this.item.get('timeout'));
    this.enable();
  }

  launchWizard() {
    const codes = getCodeOptions(this.item.get('type'));
    const criteriaType = codes ? codes[0].type : this.item.get('type');
    const criteriaSubtype = codes ? codes[0].subtype : null;
    const fullTree = this.item.get('fullTree', false);
    const {role, groupId, itemId} = this;
    const context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
    const item = this.item;
    this.actions.reOpenWizard(item, context);
  }
}
