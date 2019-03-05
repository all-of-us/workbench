import {ChangeDetectorRef, Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {
  Cohort,
  CreateReviewRequest,
} from 'generated/fetch';
import {CohortReview} from 'generated/fetch';


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
    private cdr: ChangeDetectorRef
  ) {}

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  get maxParticipants() {
    return Math.min(this.review.matchedParticipantCount, 10000);
  }

  ngOnInit() {
    this.review = cohortReviewStore.getValue();
    this.cohort = currentCohortStore.getValue();

    this.numParticipants.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(this.maxParticipants),
    ]);
  }

  cancelReview() {
    const {ns, wsid} = urlParamsStore.getValue();
    navigate(['workspaces', ns, wsid, 'cohorts']);
  }

  createReview() {
    this.creating = true;
    this.cdr.detectChanges();
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    cohortReviewApi().createCohortReview(ns, wsid, cid, cdrid, request)
      .then(_ => {
        this.creating = false;
        this.created.emit(true);
        navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
      });
  }
}
