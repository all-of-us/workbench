import {select} from '@angular-redux/store';
import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
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
import {currentCohortStore, currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';

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

  includeSize: number;
  tempLength = {};
  private subscription;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = Observable.combineLatest(
      queryParamsStore, currentWorkspaceStore
    ).subscribe(([params, workspace]) => {
      /* EVERY time the route changes, reset the store first */
      this.actions.resetStore();
      this.actions.cdrVersionId = +(workspace.cdrVersionId);

      /* If a cohort id is given in the route, we initialize state with
       * it */
      const cohortId = params.cohortId;
      if (cohortId) {
        cohortsApi().getCohort(workspace.namespace, workspace.id, cohortId)
          .then(cohort => {
            currentCohortStore.next(cohort);
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
    currentCohortStore.next(undefined);
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
