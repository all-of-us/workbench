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
import {mapRequest, parseCohortDefinition} from 'app/cohort-search/utils';
import {currentCohortStore, currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {SearchRequest} from 'generated/fetch';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  selector: 'app-cohort-search',
  templateUrl: './cohort-search.component.html',
  styleUrls: ['./cohort-search.component.css', '../../styles/buttons.css'],
})
export class CohortSearchComponent implements OnInit, OnDestroy {

  @ViewChild('wrapper') wrapper;

  private subscription;
  loading = false;
  count: number;
  error = false;
  overview = false;
  criteria = {includes: [], excludes: []};
  triggerUpdate = 0;
  cohort: any;
  resolve: Function;
  modalPromise: Promise<boolean> | null = null;
  modalOpen = false;
  saving = false;

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
      } else {
        this.cohort = {criteria: '{"includes":[],"excludes":[]}'};
      }
    });

    searchRequestStore.subscribe(sr => {
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

  canDeactivate(): Promise<boolean> | boolean {
    const criteria = JSON.stringify(mapRequest(this.criteria));
    return criteria === this.cohort.criteria || this.saving || this.showWarningModal();
  }

  async showWarningModal() {
    this.modalPromise = new Promise<boolean>((resolve => this.resolve = resolve));
    this.modalOpen = true;
    return await this.modalPromise;
  }

  getModalResponse(res: boolean) {
    this.modalOpen = false;
    this.resolve(res);
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

  updateSaving = (flag: boolean) => {
    this.saving = flag;
  }
}
