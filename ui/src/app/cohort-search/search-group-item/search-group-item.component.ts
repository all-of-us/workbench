import {Component, Input} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {
  CohortSearchActions,
  CohortSearchState,
  getItem,
  parameterList
} from '../redux';
import {SearchRequest} from 'generated';


const getDisplayName = (criteria) =>
  criteria.get('type').match(/^DEMO.*/i)
    ?  criteria.get('name')
    : criteria.get('code');


@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
  styleUrls: ['./search-group-item.component.css'],
})
export class SearchGroupItemComponent {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() itemId: string;
  @Input() itemIndex: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get codeType() {
    const _type = this.item.get('type').toUpperCase();
    return this.item.get('description', `${_type} ${this.pluralizedCode}`);
  }

  get pluralizedCode() {
    return this._rawCodes.count() > 1 ? 'Codes' : 'Code';
  }

  get item() {
    return getItem(this.itemId)(this.ngRedux.getState());
  }

  get isRequesting() {
    return this.item.get('isRequesting', false);
  }

  get _rawCodes() {
    return parameterList(this.itemId)(this.ngRedux.getState());
  }

  get codes() {
    return this._rawCodes.map(getDisplayName).join(', ');
  }

  launchWizard() {
    const criteriaType = this.item.get('type');
    const {role, groupId, itemId} = this;
    const context = {criteriaType, role, groupId, itemId};
    const item = this.item;
    this.actions.reOpenWizard(item, context);
  }
}
