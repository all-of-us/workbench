import {Component, Input, OnInit, OnDestroy} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List, Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  getItem,
  parameterList
} from '../redux';
import {SearchRequest} from 'generated';


const getDisplayName = (criteria: Map<any, any>): string =>
  criteria.get('type').match(/^DEMO.*/i)
    ?  criteria.get('name', 'N/A')
    : criteria.get('code', 'N/A');


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
    let _type = this.item.get('type', '');
    if (_type.match(/^DEMO.*/i)) {
      _type = 'Demographics';
    } else if (_type.match(/^(ICD|CPT).*/i)) {
      _type = _type.toUpperCase();
    }
    return this.item.get('description', `${_type} ${this.pluralizedCode}`);
  }

  get pluralizedCode() {
    return this.rawCodes.count() > 1 ? 'Codes' : 'Code';
  }

  get isRequesting() {
    return this.item.get('isRequesting', false);
  }

  get codes() {
    return this.rawCodes.map(getDisplayName).join(', ');
  }

  launchWizard() {
    const criteriaType = this.item.get('type');
    const {role, groupId, itemId} = this;
    const context = {criteriaType, role, groupId, itemId};
    const item = this.item;
    this.actions.reOpenWizard(item, context);
  }
}
