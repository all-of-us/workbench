import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
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
export class CohortSearchComponent implements OnInit, OnDestroy {

  @select(includeGroups) includeGroups$: Observable<List<any>>;
  @select(excludeGroups) excludeGroups$: Observable<List<any>>;
  @select(wizardOpen) open$: Observable<boolean>;
  @select(totalCount) total$: Observable<number>;
  @select(chartData) chartData$: Observable<List<any>>;
  @select(isRequstingTotal) isRequesting$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;

  private subscription;

  /* tslint:disable-next-line:no-unused-variable */
  constructor(
    private actions: CohortSearchActions,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this.route.queryParams.subscribe(params => {
      /* EVERY time the route changes, reset the store first */
      this.actions.resetStore();

      /* If a criteria string is given in the route, we initialize state with
       * it */
      const criteria = params.criteria;
      if (criteria) {
        this.actions.loadFromJSON(criteria);
        this.actions.runAllRequests();
      }
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
