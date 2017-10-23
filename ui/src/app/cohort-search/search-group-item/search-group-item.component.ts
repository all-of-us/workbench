import {Component, Input} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {
  CohortSearchActions, 
  CohortSearchState, 
  getItem, 
  parameterList
} from '../redux';

@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
})
export class SearchGroupItemComponent {
  @Input() itemId: string;
  @Input() role: string;
  @Input() groupId: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get description() {
    const _type = this.item.get('type').toUpperCase();
    return this.item.get('description', `${_type} Codes`);
  }

  get item() {
    return getItem(this.itemId)(this.ngRedux.getState());
  }

  get isRequesting() {
    return this.item.get('isRequesting', false);
  }

  get codes() {
    return parameterList(this.itemId)(this.ngRedux.getState())
      .map(param => param.get('code', 'n/a'))
      .join(', ');
  }
}
