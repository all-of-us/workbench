import {Component, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

const CDR_VERSION = 1;
import {ReviewStateService} from '../review-state.service';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  CohortStatus,
  CreateReviewRequest,
  ParticipantCohortStatus,
  ReviewStatus,
} from 'generated';

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit, OnDestroy {

  readonly CohortStatus = CohortStatus;
  private cohort: Cohort;
  private review: CohortReview;
  private subscription: Subscription;

  private createReviewModalOpen = false;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    const {ns, wsid, cid} = this.route.snapshot.params;

    /* Set these immediately on init */
    this.cohort = cohort;
    this.review = review;

    this.state.cohort.next(cohort);
    this.state.review.next(review);
    this.state.participant.next(null);
    this.state.context.next({
      cdrVersion: CDR_VERSION,
      cohortId: cid,
      workspaceId: wsid,
      workspaceNamespace: ns,
    });

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.createReviewModalOpen = true;
    }

    /* Set up listeners though */
    const resolvedReview = this.route.data.pluck('review');
    const resolvedCohort = this.route.data.pluck('cohort');

    const reviewSub = Observable
      .merge(this.state.review, resolvedReview)
      .subscribe(_review => this.review = <CohortReview>_review);

    const cohortSub = Observable
      .merge(this.state.cohort, resolvedCohort)
      .subscribe(_cohort => this.cohort = <Cohort>_cohort);

    this.subscription = reviewSub;
    this.subscription.add(cohortSub);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
