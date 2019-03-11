import {Component, OnDestroy, OnInit} from '@angular/core';
import {cohortReviewStore, filterStateStore, vocabOptions} from 'app/cohort-review/review-state.service';
import {cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {PageFilterType, ReviewStatus, SortOrder} from 'generated/fetch';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit, OnDestroy {
  reviewPresent: boolean;
  cohortLoaded = false;
  constructor() {}

  ngOnInit() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, cdrid, {
      page: 0,
      pageSize: 25,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
    }).then(review => {
      cohortReviewStore.next(review);
      if (review.reviewStatus === ReviewStatus.NONE) {
        this.reviewPresent = false;
      } else {
        this.reviewPresent = true;
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
      }
    });
    cohortsApi().getCohort(ns, wsid, cid).then(cohort => {
      // This effectively makes the 'current cohort' available to child components, by using
      // the `withCurrentCohort` HOC. In addition, this store is used to render the breadcrumb.
      currentCohortStore.next(cohort);
      this.cohortLoaded = true;
    });
    if (!vocabOptions.getValue()) {
      cohortReviewApi().getVocabularies(ns, wsid, cid, cdrId)
        .then(response => {
          const filters = {Source: {}, Standard: {}};
          response.items.forEach(item => {
            filters[item.type][item.domain] = [
              ...(filters[item.type][item.domain] || []),
              item.vocabulary
            ];
          });
          vocabOptions.next(filters);
        });
    }
  }

  ngOnDestroy() {
    currentCohortStore.next(undefined);
    filterStateStore.next(null);
  }

  reviewCreated() {
    this.reviewPresent = true;
  }
}
