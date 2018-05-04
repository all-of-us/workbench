import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

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
  cdrId: number;
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
    private workspaceStorageService: WorkspaceStorageService,
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

    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
    this.workspaceStorageService.reloadIfNew(
      this.route.snapshot.parent.params['ns'],
      this.route.snapshot.parent.params['wsid']);
  }

  cancelReview() {
    const {ns, wsid} = this.route.parent.snapshot.parent.params;
    console.dir(this.route);
    this.router.navigate(['workspace', ns, wsid]);
  }

  createReview() {
    this.creating = true;
    const {ns, wsid, cid} = this.route.parent.snapshot.parent.params;
    const request = <CreateReviewRequest>{size: this.numParticipants.value};

    this.reviewAPI.createCohortReview(ns, wsid, cid, this.cdrId, request)
      .subscribe(_ => {
        this.creating = false;
        this.router.navigate(['overview'], {relativeTo: this.route.parent});
      });
  }
}
