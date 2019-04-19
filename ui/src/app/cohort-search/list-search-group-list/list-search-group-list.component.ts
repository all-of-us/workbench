import {select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-list-search-group-list',
  templateUrl: './list-search-group-list.component.html',
  styleUrls: [
    './list-search-group-list.component.css',
  ]
})
export class ListSearchGroupListComponent implements OnInit {
  @Input() role: keyof SearchRequest;
  @Output() tempLength = new EventEmitter<boolean>();

  @select(s => s.get('initShowChart', true)) initShowChart$: Observable<boolean>;
  groups: any;
  index = 0;
  subscription: Subscription;

  ngOnInit(): void {
    this.subscription = searchRequestStore
      .filter(sr => !!sr)
      .subscribe(searchRequest => {
        console.log(searchRequest);
        this.groups = searchRequest[this.role];
        if (this.role === 'excludes') {
          this.index = searchRequest.includes.length + 1;
        }
      });
  }

  get title() {
    const prefix = this.role === 'excludes' ? 'And ' : '';
    return prefix + this.role.slice(0, -1) + ` Participants`;
  }

  get emptyIndex() {
    return this.groups.length + this.index + 1;
  }
  getTemporalLength(e) {
    this.tempLength.emit(e);
  }
}
