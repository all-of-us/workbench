import {
  Component,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Router, ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
  includeGroups,
  excludeGroups,
  wizardOpen,
  totalCount,
  isRequstingTotal,
  chartData,
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

  private adding = false;
  private subscriptions: Subscription[];

  constructor(
    private actions: CohortSearchActions,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  save(): void {
    if (this.adding) {
      this.router.navigate(['../create'], {relativeTo : this.route});
    } else {
      this.router.navigate(['../edit'], {relativeTo : this.route});
    }
  }
}
