import {Component, Input, EventEmitter, Output} from '@angular/core';

import {SearchRequest} from 'generated';

@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
})
export class SearchGroupComponent {
  @Input() group;
  @Input() role: keyof SearchRequest;
  @Output() onRemove = new EventEmitter<boolean>();

  remove(event) { this.onRemove.emit(true); }
}
