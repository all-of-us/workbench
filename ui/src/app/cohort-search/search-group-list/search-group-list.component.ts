import {select} from '@angular-redux/store';
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group-list',
  templateUrl: './search-group-list.component.html',
  styleUrls: [
    './search-group-list.component.css',
  ]
})
export class SearchGroupListComponent {
  @Input() role: keyof SearchRequest;
  @Input() groups$: Observable<List<any>>;
  @Output() tempLength = new EventEmitter<boolean>();

  @select(s => s.get('initShowChart', true)) initShowChart$: Observable<boolean>;

  get title() {
    const prefix = this.role === 'excludes' ? 'And ' : '';
    return prefix + this.role.slice(0, -1) + ` Participants`;
  }
  getTemporalLength(e) {
      this.tempLength.emit(e);
  }
}
