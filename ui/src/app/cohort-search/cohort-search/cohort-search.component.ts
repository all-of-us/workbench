import {select} from '@angular-redux/store';
import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortsService} from 'generated';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {
  chartData,
  CohortSearchActions,
  excludeGroups,
  includeGroups,
  isRequstingTotal,
  totalCount,
} from 'app/cohort-search/redux';

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
  @select(totalCount) total$: Observable<number>;
  @select(chartData) chartData$: Observable<List<any>>;
  @select(isRequstingTotal) isRequesting$: Observable<boolean>;
  @select(s => s.get('initShowChart', true)) initShowChart$: Observable<boolean>;

  @ViewChild('wrapper') wrapper;
  tempLength= {};

  includeSize: number;
  private subscription;

  constructor(
    private actions: CohortSearchActions,
    private api: CohortsService,
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

      /* If a cohort id is given in the route, we initialize state with
       * it */
      const cohortId = params.cohortId;
      if (cohortId) {
        this.api.getCohort(data.workspace.namespace, data.workspace.id, cohortId)
          .subscribe(cohort => {
            if (cohort.criteria) {
              this.actions.loadFromJSON(cohort.criteria);
              this.actions.runAllRequests();
            }
          });
      }
    });
    this.subscription.add(
      this.includeGroups$.subscribe(groups => this.includeSize = groups.size + 1)
    );
    this.updateWrapperDimensions();
  }

  ngOnDestroy() {
    this.actions.clearStore();
    this.subscription.unsubscribe();
  }

  getTempObj(e) {
    this.tempLength = e;
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
