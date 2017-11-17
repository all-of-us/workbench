import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {List, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  getItem,
  parameterList
} from '../redux';

import {
  attributeDisplay,
  nameDisplay,
  typeDisplay,
  typeToTitle,
} from '../utils';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
  styleUrls: ['./search-group-item.component.css'],
})
export class SearchGroupItemComponent implements OnInit, OnDestroy {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() itemId: string;
  @Input() itemIndex: number;

  private item: Map<any, any> = Map();
  private rawCodes: List<any> = List();
  private _subscriptions: Subscription[];

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    const select = this.ngRedux.select;
    this._subscriptions = [
      select(getItem(this.itemId)).subscribe(item => this.item = item),
      select(parameterList(this.itemId)).subscribe(rawCodes => this.rawCodes = rawCodes),
    ];
  }

  ngOnDestroy() {
    this._subscriptions.forEach(sub => sub.unsubscribe());
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

  get codes() {
    const _type = this.codeType;
    const _formatter = (param) => {
      const funcs = _type === 'Demographics'
        ? [typeDisplay, nameDisplay, attributeDisplay]
        : [typeDisplay, attributeDisplay];
      return funcs.map(f => f(param)).join(' ').trim();
    };
    const sep = _type === 'Demographics' ? '; ' : ', ';
    return this.rawCodes.map(_formatter).join(sep);
  }

  remove() {
    this.actions.removeGroupItem(this.role, this.groupId, this.itemId);
  }

  launchWizard() {
    const criteriaType = this.item.get('type');
    const {role, groupId, itemId} = this;
    const context = {criteriaType, role, groupId, itemId};
    const item = this.item;
    this.actions.reOpenWizard(item, context);
  }
}
