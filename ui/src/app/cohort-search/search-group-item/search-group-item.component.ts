import {Component, Input} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {CohortSearchActions, CohortSearchState, getItem} from '../redux';

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
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }

  get item() {
    return getItem(this.itemId)(this.ngRedux.getState());
  }
}
