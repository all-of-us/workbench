import {Component, Input, EventEmitter, Output} from '@angular/core';

import {BroadcastService} from '../broadcast.service';
import {SearchResult} from '../model';

@Component({
  selector: 'app-search-result',
  templateUrl: 'search-result.component.html',
  styleUrls: ['search-result.component.css']
})
export class SearchResultComponent {

  @Input()
  searchResult: SearchResult;
  @Input()
  index: number;
  @Output()
  onRemove = new EventEmitter<SearchResult>();

  constructor(private broadcastService: BroadcastService) {}

  removeSearchResult(searchResult: SearchResult) {
    this.onRemove.emit(searchResult);
  }

  selectSearchResult() {
    this.broadcastService.selectSearchResult(this.searchResult);
  }
}
