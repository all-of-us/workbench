import {Component, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  CohortStatus,
  CreateReviewRequest,
  ParticipantCohortStatus,
  ReviewStatus,
} from 'generated';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit, OnDestroy {
  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  private cohort: Cohort;
  private review: CohortReview;

  private open = false;
  private loading = false;
  private subscription: Subscription;

  @ViewChild('wrapper') _wrapper;
  @ViewChild('subjectNav') _subjectNav;
  @ViewChild('openNav') _openNav;
  @ViewChild('createCohortModal') createCohortModal;

  get wrapper() { return this._wrapper.nativeElement; }
  get openNav() { return this._openNav.nativeElement; }
  get subjectNav() { return this._subjectNav.nativeElement; }
  get numParticipants() { return this.reviewParamForm.get('numParticipants'); }

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this._updateWrapperDimensions();
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
    const params = this.route.snapshot.params;
    /* TODO: wire up CDR version */
    this.reviewAPI.createCohortReview(
      params.ns,
      params.wsid,
      params.cid,
      1,
      <CreateReviewRequest>{size: this.numParticipants.value},
    ).subscribe(review => {
      this.review = review;
      this.createCohortModal.close();
    });
  }

  @HostListener('document:click', ['$event'])
  onClick(event) {
    if (this.subjectNav.contains(event.target)
        || this.openNav.contains(event.target)) {
      return;
    }
    this.open = false;
  }

  @HostListener('window:resize')
  onResize() {
    this._updateWrapperDimensions();
  }

  _updateWrapperDimensions() {
    const {top} = this.wrapper.getBoundingClientRect();
    this.wrapper.style.minHeight = pixel(window.innerHeight - top - ONE_REM);
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
