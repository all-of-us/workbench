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
  styles: [`
    .flex-container {
      display: flex;
    }
    .flex-container > clr-tooltip {
      min-width: 0;
      flex: 4;
    }
    .flex-container > div.count {
      flex: 1;
      text-align: right;
    }
    .line-item {
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
    .trigger:hover > span {
      color: rgb(0, 124, 187)!important;
    }
  `]
})
export class SearchGroupItemComponent {
  @Input() itemId: string;
  @Input() role: string;
  @Input() groupId: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get codeType() {
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
