import {ChangeDetectorRef, Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';


@Component({
  selector: 'app-create-review-page',
  templateUrl: './create-review-page.html',
  styleUrls: ['./create-review-page.css']
})
export class CreateReviewPage implements OnInit {
  create = true;
  creating = false;
  cohort: Cohort;
  review: CohortReview;
  @Output() created = new EventEmitter<boolean>();

  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  constructor(
    private reviewAPI: CohortReviewService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    return Math.min(this.review.matchedParticipantCount, 10000);
  }

  ngOnInit() {
    const {review, cohort} = this.route.snapshot.data;
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
    this.router.navigate(['workspaces', ns, wsid, 'cohorts']);
  }

  createReview() {
    this.creating = true;
    this.cdr.detectChanges();
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = this.route.parent.snapshot.data.workspace.cdrVersionId;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    this.reviewAPI.createCohortReview(ns, wsid, cid, cdrid, request)
      .subscribe(_ => {
        this.creating = false;
        this.created.emit(true);
        this.router.navigate(['participants'], {relativeTo: this.route.parent});
      });
  }
}
