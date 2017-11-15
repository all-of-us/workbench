import {Component, Input} from '@angular/core';

@Component({
  selector: 'crit-quicksearch-results',
  templateUrl: './quicksearch-results.component.html',
  styleUrls: ['./quicksearch-results.component.css']
})
export class QuickSearchResultsComponent {
  @Input() results;
}
