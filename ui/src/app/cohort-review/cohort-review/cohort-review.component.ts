import {Component, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

const CDR_VERSION = 1;

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
  private participantPage: ParticipantCohortStatus[];

  private subscription: Subscription;
  private _selectedParticipant: ParticipantCohortStatus | null;

  private loading = false;

  @ViewChild('createCohortModal') createCohortModal;

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.subscription = this.route.data.subscribe(({cohort, review}) => {
      this.cohort = cohort;
      this.review = review;
      if (review.reviewStatus === ReviewStatus.NONE) {
        this.createCohortModal.open();
        this.numParticipants.setValidators(Validators.compose([
          Validators.required,
          Validators.min(1),
          Validators.max(this.maxParticipants),
        ]));
      }
    });
  }

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    if (this.review && this.review.matchedParticipantCount) {
      return Math.min(10000, this.review.matchedParticipantCount);
    }
    return 10000;
  }

  get selectedParticipant() {
    return this._selectedParticipant;
  }

  set selectedParticipant(selection: ParticipantCohortStatus) {
    const ind = this.review.participantCohortStatuses.findIndex(
      ({participantId}) => participantId === selection.participantId
    );
    if (ind > 0) {
      // If the selected ID is on the current page, "optimistically" update the page
      this.review.participantCohortStatuses.splice(ind, 1, selection);
    }
    this._selectedParticipant = selection;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancelReview() {
    const params = this.route.snapshot.params;
    this.router.navigate(['workspace', params.ns, params.wsid]);
  }

  createReview() {
    console.log('Creating review... ');
    const {ns, wsid, cid} = this.route.snapshot.params;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};
    this.reviewAPI
      .createCohortReview(ns, wsid, cid, CDR_VERSION, request)
      .subscribe(review => {
        this.review = review;
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
