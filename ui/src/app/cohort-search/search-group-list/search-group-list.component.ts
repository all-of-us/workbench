import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List} from 'immutable';

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
    const verb = this.role.charAt(0).toUpperCase() + this.role.slice(1, -1) + 'd';
    return `${verb} Participants`;
  }
}
