import {select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-list',
  templateUrl: './search-group-list.component.html',
  styleUrls: [
    './search-group-list.component.css',
  ]
})
export class SearchGroupListComponent implements OnInit {
  @Input() role: keyof SearchRequest;
  @Input() groups$: Observable<List<any>>;
  @Input() index: number;
  @Output() tempLength = new EventEmitter<boolean>();

  @select(s => s.get('initShowChart', true)) initShowChart$: Observable<boolean>;
  listSize: number;
  subscription: Subscription;

  ngOnInit(): void {
    this.subscription = this.groups$.subscribe(groups => this.listSize = groups.size);
  }

  get title() {
    const prefix = this.role === 'excludes' ? 'And ' : '';
    return prefix + this.role.slice(0, -1) + ` Participants`;
  }

  get emptyIndex() {
    return this.listSize + this.index + 1;
  }
  getTemporalLength(e) {
    this.tempLength.emit(e);
  }
}
