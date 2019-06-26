import {Component, Input} from '@angular/core';
import {SearchRequest} from 'generated';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-list-search-group-list',
  templateUrl: './list-search-group-list.component.html',
  styleUrls: [
    './list-search-group-list.component.css',
  ]
})
export class ListSearchGroupListComponent {
  @Input() role: keyof SearchRequest;
  @Input() groups: Array<any>;

  index = 0;
  subscription: Subscription;

  get title() {
    const prefix = this.role === 'excludes' ? 'And ' : '';
    return prefix + this.role.slice(0, -1) + ` Participants`;
  }

  get emptyIndex() {
    return this.groups.length + this.index + 1;
  }
}
