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

  subjectStatus = new FormControl();

  private cohort: Cohort;
  private review: CohortReview;
  private loading = false;
  private subscription: Subscription;

  @ViewChild('createCohortModal') createCohortModal;

  get numParticipants() { return this.reviewParamForm.get('numParticipants'); }

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

    const [detailview, overview] = this.route.firstChild.params
      .map(params => params.subjectID)
      .distinctUntilChanged()
      .partition(id => id);  // undefined means overview, not detail view

    const detailviewSub = detailview
      .map(id => +id)
      .map(id => (pstat: ParticipantCohortStatus): boolean => pstat.participantId === id)
      .map(finder => this.review.participantCohortStatuses.find(finder))
      .subscribe(subject => {
        console.log(`DetailView changing to participant: ${subject.participantId}`);
      });

    // const statusChangerSub = this.subjectStatus.valueChanges
    //   .switchMap(status) => {
    //     const request = <ModifyCohortStatusRequest>{status};
    //     const {ns, wsid, cid} = this.route.snapshot.params;
    //     return this.reviewAPI.updateParticipantCohortStatus(ns, wsid, cid, CDR_VERSION, request);
    //   })
    //   .map(resp =>
    //     this.review.participantCohortStatuses.map(statObj =>
    //       statObj.participantId === resp.participantId
    //         ? resp
    //         : statObj
    //   ))
    //   .subscribe(newStatusSet => this.review.participantCohortStatuses = newStatusSet);

    this.subscription.add(detailviewSub);
    // this.subscription.add(statusChangerSub);
  }

  get maxParticipants() {
    if (this.review && this.review.matchedParticipantCount) {
      return Math.min(10000, this.review.matchedParticipantCount);
    }
    return 10000;
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
