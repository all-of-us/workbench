import {select} from '@angular-redux/store';
import {Component, Input} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {activeParameterList} from '../../redux';

@Component({
  selector: 'crit-quicksearch-results',
  templateUrl: './quicksearch-results.component.html',
  styleUrls: ['./quicksearch-results.component.css']
})
export class QuickSearchResultsComponent {
  @Input() results: List<any>;
  @select(activeParameterList) selected$: Observable<List<any>>;
}
