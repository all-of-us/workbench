import {Component, OnDestroy, OnInit} from '@angular/core';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {CohortReviewService, CohortsService, PageFilterRequest, PageFilterType, ParticipantCohortStatusColumns, ReviewStatus, SortOrder} from 'generated';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit, OnDestroy {
  reviewPresent: boolean;
  cohortLoaded = false;
  constructor(
    private reviewAPI: CohortReviewService,
    private cohortsAPI: CohortsService
  ) {}

  ngOnInit() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, cdrid, {
      page: 0,
      pageSize: 25,
      sortColumn: ParticipantCohortStatusColumns.PARTICIPANTID,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
    } as PageFilterRequest).subscribe(review => {
      cohortReviewStore.next(review);
      if (review.reviewStatus === ReviewStatus.NONE) {
        this.reviewPresent = false;
      } else {
        this.reviewPresent = true;
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
      }
    });
    this.cohortsAPI.getCohort(ns, wsid, cid).subscribe(cohort => {
      currentCohortStore.next(cohort);
      this.cohortLoaded = true;
    });
  }

  ngOnDestroy() {
    currentCohortStore.next(undefined);
  }

  reviewCreated() {
    this.reviewPresent = true;
  }
}
