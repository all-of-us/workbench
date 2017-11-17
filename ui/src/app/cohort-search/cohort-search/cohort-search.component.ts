import {select} from '@angular-redux/store';
import {Component} from '@angular/core';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {
  activeCriteriaType,
  chartData,
  CohortSearchActions,
  excludeGroups,
  includeGroups,
  isRequstingTotal,
  totalCount,
  wizardOpen,
} from '../redux';

@Component({
  selector: 'app-cohort-search',
  templateUrl: './cohort-search.component.html',
  styleUrls: ['./cohort-search.component.css'],
})
export class CohortSearchComponent {

  @select(includeGroups) includeGroups$: Observable<List<any>>;
  @select(excludeGroups) excludeGroups$: Observable<List<any>>;
  @select(wizardOpen) open$: Observable<boolean>;
  @select(totalCount) total$: Observable<number>;
  @select(chartData) chartData$: Observable<List<any>>;
  @select(isRequstingTotal) isRequesting$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;

  /* tslint:disable-next-line:no-unused-variable */
  constructor(private actions: CohortSearchActions) {}
}
