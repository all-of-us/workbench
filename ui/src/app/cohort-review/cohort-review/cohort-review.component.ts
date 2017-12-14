import {Component, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
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
  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });
  private cohort: Cohort;
  private review: CohortReview;
  private subscription: Subscription;
  @ViewChild('createCohortModal') createCohortModal;
  private buttonBusy = false;

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
      this.createCohortModal.open();
      this.numParticipants.setValidators(Validators.compose([
        Validators.required,
        Validators.min(1),
        Validators.max(Math.min(10000, review.matchedParticipantCount)),
      ]));
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

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  cancelReview() {
    const params = this.route.snapshot.params;
    this.router.navigate(['workspace', params.ns, params.wsid]);
  }

  createReview() {
    console.log('Creating review... ');
    const {ns, wsid, cid} = this.route.snapshot.params;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};
    const buttonBusy = true;
    this.reviewAPI
      .createCohortReview(ns, wsid, cid, CDR_VERSION, request)
      .subscribe(review => {
        this.review = review;
        this.buttonBusy = false;
        this.state.review.next(review);
        this.createCohortModal.close();
      });
  }

  statusText(stat: CohortStatus): string {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Undecided',
      [CohortStatus.NOTREVIEWED]: 'Unreviewed',
    }[stat];
  }

  statusClass(stat: CohortStatus) {
    if (stat === CohortStatus.INCLUDED) {
      return {'label-success': true};
    } else if (stat === CohortStatus.EXCLUDED) {
      return {'label-warning': true};
    } else {
      return {'label-info': true};
    }
  }
}
