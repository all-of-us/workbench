import {Component, Input} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from '../redux';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-list',
  templateUrl: './search-group-list.component.html',
  styleUrls: ['./search-group-list.component.css']
})
export class SearchGroupListComponent {
  @Input() role: keyof SearchRequest;
  @Input() groups$: Observable<List<any>>;

  constructor(private actions: CohortSearchActions) {}

  initGroup() {
    const newId = this.actions.generateId(this.role);
    this.actions.initGroup(this.role, newId);
  }

  get title() {
    return this.role.slice(0, -1) + ` Participants Where`;
  }
}
