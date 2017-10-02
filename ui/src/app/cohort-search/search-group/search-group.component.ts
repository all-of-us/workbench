import {Component, Input, EventEmitter, Output} from '@angular/core';
import {NgRedux, select, dispatch} from '@angular-redux/store';

import {BroadcastService} from '../broadcast.service';
import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store';
import {SearchResult} from '../model';

import {SearchGroup} from 'generated';


@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
  styleUrls: ['search-group.component.css']
})
export class SearchGroupComponent {
  @Input() index: number;
  @Input() group: SearchGroup;
  @Output() onRemove = new EventEmitter<boolean>();

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  remove(event) { this.onRemove.emit(true); }
  selectSearchGroup() {}
}
