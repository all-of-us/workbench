import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {Observable} from 'rxjs/Observable';

import {idsInUse, initExisting, searchRequestStore} from 'app/cohort-search/search-state.service';
import {parseCohortDefinition} from 'app/cohort-search/utils';
import {currentCohortStore, currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {SearchRequest} from 'generated/fetch';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  selector: 'app-cohort-search',
  templateUrl: './cohort-search.component.html',
  styleUrls: ['./cohort-search.component.css'],
})
export class CohortSearchComponent implements OnInit, OnDestroy {

  @ViewChild('wrapper') wrapper;

  includeSize: number;
  tempLength = {};
  private subscription;
  loading = false;
  count: number;
  error = false;
  overview = false;
  criteria = {includes: [], excludes: []};
  triggerUpdate = 0;
  cohort: any;

  ngOnInit() {
    this.subscription = Observable.combineLatest(
      queryParamsStore, currentWorkspaceStore
    ).subscribe(([params, workspace]) => {
      /* If a cohort id is given in the route, we initialize state with
       * it */
      const cohortId = params.cohortId;
      if (cohortId) {
        this.loading = true;
        cohortsApi().getCohort(workspace.namespace, workspace.id, cohortId)
          .then(cohort => {
            this.loading = false;
            this.cohort = cohort;
            currentCohortStore.next(cohort);
            if (cohort.criteria) {
              initExisting.next(true);
              searchRequestStore.next(parseCohortDefinition(cohort.criteria));
            }
          });
      }
    });

    searchRequestStore.subscribe(sr => {
      this.includeSize = sr.includes.length;
      this.criteria = sr;
      this.overview = sr.includes.length || sr.excludes.length;
    });
    this.updateWrapperDimensions();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    idsInUse.next(new Set());
    currentCohortStore.next(undefined);
    searchRequestStore.next({includes: [], excludes: []} as SearchRequest);
  }

  canDeactivate(): Observable<boolean> | boolean {
    return true;
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

  updateRequest = () => {
    this.triggerUpdate++;
  }
}
