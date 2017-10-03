import {Component, Input} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store';
import {SearchGroupItem} from 'generated';


@Component({
  selector: 'app-search-group-item',
  templateUrl: './component.html',
})
export class SearchGroupItemComponent {
  @Input() item;
  @Input() role: string;
  @Input() index: number;
  @Input() itemIndex: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get description() {
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }

  get count() {
    return this.ngRedux.getState().getIn(
      ['results', this.role, this.index, this.itemIndex, 'count'],
      0 // default
    );
  }
}
