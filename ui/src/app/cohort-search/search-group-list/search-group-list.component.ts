import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {SearchRequest} from 'generated';
import {Subscription} from 'rxjs/Subscription';

import {searchRequestStore} from 'app/cohort-search/search-state.service';

@Component({
  selector: 'app-list-search-group-list',
  templateUrl: './search-group-list.component.html',
  styleUrls: [
    './search-group-list.component.css',
  ]
})
export class SearchGroupListComponent implements OnChanges, OnInit {
  @Input() role: keyof SearchRequest;
  @Input() groups: Array<any>;
  @Input() updated: number;
  @Input() updateRequest: Function;

  index = 0;
  subscription: Subscription;
  updateGroups = 0;

  ngOnInit(): void {
    if (this.role === 'excludes') {
      searchRequestStore.subscribe(sr => this.index = sr.includes.length + 1);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.updated) {
      // Trigger child search-groups to update props
      this.updateGroups++;
    }
  }

  get title() {
    const prefix = this.role === 'excludes' ? 'And ' : '';
    return prefix + this.role.slice(0, -1) + ` Participants`;
  }

  get emptyIndex() {
    return this.groups.length + this.index + 1;
  }
}
