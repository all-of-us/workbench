import {Component, Input, EventEmitter, Output} from '@angular/core';

import {CohortSearchActions} from '../actions';
import {CohortSearchState, SearchGroupRole} from '../store.interfaces';

import {SearchGroup} from 'generated';


@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
  styleUrls: ['search-group.component.css']
})
export class SearchGroupComponent {
  /* Passed through from cohort-builder to add-criteria */
  @Input() index: number;
  @Input() role: SearchGroupRole;

  @Input() group: SearchGroup;
  @Output() onRemove = new EventEmitter<boolean>();

  constructor(private actions: CohortSearchActions) {}

  remove(event) { this.onRemove.emit(true); }
}
