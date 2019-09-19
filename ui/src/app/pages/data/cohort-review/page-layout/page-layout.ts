import {Location} from '@angular/common';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {NavigationStart, Router} from '@angular/router';
import {
  cohortReviewStore,
  filterStateStore,
  initialFilterState,
  multiOptions,
  visitsFilterOptions,
  vocabOptions
} from 'app/services/review-state.service';
import {cohortBuilderApi, cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {PageFilterType, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit, OnDestroy {
  reviewPresent: boolean;
  cohortLoaded = false;
  readonly = false;
  subscription: Subscription;

  constructor(private location: Location, private router: Router) {
    this.subscription = router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        const oldRoute = router.url.split('/').pop();
        const newRoute = event.url.split('/').pop();
        if (oldRoute === 'participants' && newRoute === 'review') {
          // back button was pressed on 'participants' route, go back again to avoid blank page
          location.back();
        }
      }
    });
  }

  ngOnInit() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const {accessLevel, cdrVersionId} = currentWorkspaceStore.getValue();
    this.readonly = accessLevel === WorkspaceAccessLevel.READER;
    cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
      page: 0,
      pageSize: 25,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
      filters: {items: []}
    }).then(review => {
      cohortReviewStore.next(review);
      this.reviewPresent = review.reviewStatus !== ReviewStatus.NONE;
      if (this.reviewPresent && this.router.url.split('/').pop() === 'review') {
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
      }
    });
    cohortsApi().getCohort(ns, wsid, cid).then(cohort => {
      // This effectively makes the 'current cohort' available to child components, by using
      // the `withCurrentCohort` HOC. In addition, this store is used to render the breadcrumb.
      currentCohortStore.next(cohort);
      this.cohortLoaded = true;
    });
    if (!visitsFilterOptions.getValue()) {
      cohortBuilderApi().getCriteriaBy(
        +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
      ).then(response => {
        visitsFilterOptions.next([
          {value: null, label: 'Any'},
          ...response.items.map(option => {
            return {value: option.name, label: option.name};
          })
        ]);
      });
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    currentCohortStore.next(undefined);
    multiOptions.next(null);
    vocabOptions.next(null);
    filterStateStore.next(JSON.parse(JSON.stringify(initialFilterState)));
  }

  reviewCreated = () => {
    this.reviewPresent = true;
  }

  goBack = () => {
    this.location.back();
  }

  get ableToReview() {
    return this.cohortLoaded && this.reviewPresent === false && !this.readonly;
  }

  get unableToReview() {
    return this.cohortLoaded && this.reviewPresent === false && this.readonly;
  }
}
