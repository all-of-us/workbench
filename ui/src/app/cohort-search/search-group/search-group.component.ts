import {Component, Input, EventEmitter, Output} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Map, Set} from 'immutable';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store';

import {SearchGroup, SearchRequest} from 'generated';


@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
  styleUrls: ['search-group.component.css']
})
export class SearchGroupComponent {
  /* Passed through from cohort-builder to add-criteria */
  @Input() index: number;
  @Input() role: keyof SearchRequest;

  @Input() group: SearchGroup;
  @Output() onRemove = new EventEmitter<boolean>();

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  remove(event) { this.onRemove.emit(true); }

  get count() {
    return Set.union(
      this.ngRedux.getState()
        .getIn(['results', this.role, this.index], Map())
        .map(item => item.get('subjects'))
    ).size;
  }
}
