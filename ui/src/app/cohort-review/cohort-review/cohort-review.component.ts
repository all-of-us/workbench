import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

const CDR_VERSION = 1;

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  private createReviewModalOpen = false;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    const {ns, wsid, cid} = this.route.snapshot.params;

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
  }
}
