import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';


@Component({
  templateUrl: './create-review-page.html',
})
export class CreateReviewPage implements OnInit {
  creating = false;
  cohort: Cohort;
  review: CohortReview;

  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  constructor(
    private reviewAPI: CohortReviewService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    return Math.min(this.review.matchedParticipantCount, 10000);
  }

  ngOnInit() {
    const {review, cohort} = this.route.parent.snapshot.data;
    this.review = review;
    this.cohort = cohort;

    this.numParticipants.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(this.maxParticipants),
    ]);
  }

  cancelReview() {
    const {ns, wsid} = this.route.parent.snapshot.params;
    console.dir(this.route);
    this.router.navigate(['workspace', ns, wsid]);
  }

  createReview() {
    this.creating = true;
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = this.route.parent.snapshot.data.workspace.cdrVersionId;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    this.reviewAPI.createCohortReview(ns, wsid, cid, cdrid, request)
      .subscribe(_ => {
        this.creating = false;
        this.router.navigate(['overview'], {relativeTo: this.route.parent});
      });
  }
}
