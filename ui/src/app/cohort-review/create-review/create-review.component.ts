import {
  Component,
  OnInit,
  ViewChild,
} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';

const CDR_VERSION = 1;

@Component({
  selector: 'app-create-review',
  templateUrl: './create-review.component.html',
  styleUrls: ['./create-review.component.css']
})
export class CreateReviewComponent implements OnInit {
  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  @ViewChild('modal') modal;

  private creating = false;
  private maxParticipants: number;
  private matchedParticipantCount: number;
  private cohortName: string;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private router: Router,
    private route: ActivatedRoute,
  ) { }

  get numParticipants() {
    return this.reviewParamForm.get('numParticipants');
  }

  ngOnInit() {
    this.state.review$
      .take(1)
      .pluck('matchedParticipantCount')
      .do((count: number) => this.matchedParticipantCount = count)
      .map((count: number) => Math.min(10000, count))
      .do((count: number) => this.maxParticipants = count)
      .map(count => Validators.compose([
        Validators.required,
        Validators.min(1),
        Validators.max(count)]))
      .subscribe(validators => this.numParticipants.setValidators(validators));

    this.state.cohort$
      .take(1)
      .pluck('name')
      .subscribe((name: string) => this.cohortName = name);
  }

  cancelReview() {
    const {ns, wsid} = this.route.snapshot.params;
    this.router.navigate(['workspace', ns, wsid]);
  }

  createReview() {
    this.creating = true;
    const {ns, wsid, cid} = this.route.snapshot.params;

    Observable.of(<CreateReviewRequest>{size: this.numParticipants.value})
      .mergeMap(request => this.reviewAPI.createCohortReview(ns, wsid, cid, CDR_VERSION, request))
      .subscribe(review => {
        this.creating = false;
        this.state.review.next(review);
        this.modal.close();
      });
  }
}
