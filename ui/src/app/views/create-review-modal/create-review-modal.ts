import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
  selector: 'app-create-review-modal',
  templateUrl: './create-review-modal.html',
  styleUrls: ['./create-review-modal.css']
})
export class CreateReviewModalComponent implements OnInit {
  @Input() cohort: Cohort;
  create = true;
  creating = false;
  review: CohortReview;

  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  constructor(
    private reviewAPI: CohortReviewService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  open(): void {
    this.creating = true;
    console.log(this.cohort);
  }

  close(): void {
    this.creating = false;
  }

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    return 10000;
    // return Math.min(this.review.matchedParticipantCount, 10000);
  }

  ngOnInit() {
    console.log(this);
    // const {review, cohort} = this.route.snapshot.data;
    // this.review = review;
    // this.cohort = cohort;

    this.numParticipants.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(this.maxParticipants),
    ]);
  }

  cancelReview() {
    const {ns, wsid} = this.route.parent.snapshot.params;
    console.dir(this.route);
    this.router.navigate(['workspaces', ns, wsid, 'cohorts']);
  }

  createReview() {
    this.creating = true;
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = this.route.parent.snapshot.data.workspace.cdrVersionId;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    this.reviewAPI.createCohortReview(ns, wsid, cid, cdrid, request)
      .subscribe(_ => {
        this.creating = false;
        // this.created.emit(true);
        this.router.navigate(['participants'], {relativeTo: this.route.parent});
      });
  }
}
