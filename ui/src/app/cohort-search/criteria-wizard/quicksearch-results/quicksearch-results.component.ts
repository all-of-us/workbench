import {Component, Input} from '@angular/core';
import {List} from 'immutable';

@Component({
  selector: 'crit-quicksearch-results',
  templateUrl: './quicksearch-results.component.html',
})
export class QuickSearchResultsComponent {
  @Input() results: List<any>;
}
