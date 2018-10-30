import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  CreateReviewRequest,
  PageFilterRequest,
  PageFilterType,
  ReviewStatus,
  SortOrder,
} from 'generated';


@Component({
  selector: 'app-create-review-modal',
  templateUrl: './create-review-modal.html',
  styleUrls: ['./create-review-modal.css']
})
export class CreateReviewModalComponent implements OnInit {
  @Input() cohort: Cohort;
  loading = false;
  create = false;
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
    this.create = true;
    this.loading = true;
    const {ns, wsid} = this.route.parent.snapshot.params;
    const cid = this.cohort.id;
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    const request = <PageFilterRequest>{
      page: 0,
      pageSize: 0,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses
    };
    this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, cdrid, request)
      .subscribe(review => {
        this.loading = false;
        if (review.reviewStatus === ReviewStatus.CREATED) {
          const url = '/workspaces/' + ns + '/' + wsid + '/cohorts/' + cid + '/review/participants';
          this.router.navigateByUrl(url);
        } else {
          this.review = review;
          this.numParticipants.setValidators([
            Validators.required,
            Validators.min(1),
            Validators.max(this.maxParticipants),
          ]);
        }
      });
  }

  close(): void {
    this.create = false;
  }

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    return Math.min(this.review.matchedParticipantCount, 10000);
  }

  ngOnInit() {

  }

  createReview() {
    this.creating = true;
    const {ns, wsid} = this.route.parent.snapshot.params;
    const cid = this.cohort.id;
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    this.reviewAPI.createCohortReview(ns, wsid, cid, cdrid, request)
      .subscribe(_ => {
        this.creating = false;
        const url = '/workspaces/' + ns + '/' + wsid + '/cohorts/' + cid + '/review/participants';
        this.router.navigateByUrl(url);
      });
  }
}
