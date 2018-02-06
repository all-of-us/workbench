import {select} from '@angular-redux/store';
import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
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

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

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

  @ViewChild('wrapper') wrapper;

  private subscription;

  constructor(
    private actions: CohortSearchActions,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    console.log(`Entering CohortSearchComponent.ngOnInit with route:`);
    console.dir(this.route);

    const {queryParams: query$, data: data$} = this.route;
    this.subscription = Observable.combineLatest(query$, data$).subscribe(([params, data]) => {
      /* EVERY time the route changes, reset the store first */
      this.actions.resetStore();
      this.actions.cdrVersionId = data.workspace.cdrVersionId;

      /* If a criteria string is given in the route, we initialize state with
       * it */
      const criteria = params.criteria;
      if (criteria) {
        this.actions.loadFromJSON(criteria);
        this.actions.runAllRequests();
      }
    });
    this.updateWrapperDimensions();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  @HostListener('window:resize')
  onResize() {
    this.updateWrapperDimensions();
  }

  updateWrapperDimensions() {
    const wrapper = this.wrapper.nativeElement;

    const {top} = wrapper.getBoundingClientRect();
    wrapper.style.minHeight = pixel(window.innerHeight - top - ONE_REM);
  }
}
