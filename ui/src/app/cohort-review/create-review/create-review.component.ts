import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';

@Component({
  selector: 'app-create-review',
  templateUrl: './create-review.component.html',
  styleUrls: ['./create-review.component.css']
})
export class CreateReviewComponent implements OnInit {
  reviewParamForm = new FormGroup({
    numParticipants: new FormControl(),
  });

  @Input() open = false;
  @Output() openChange = new EventEmitter<boolean>();

  private creating = false;
  private maxParticipants: number;
  private matchedParticipantCount: number;
  private cohortName: string;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private router: Router,
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
    this.state.context$
      .take(1)
      .map(({workspaceNamespace, workspaceId}) => ['workspace', workspaceNamespace, workspaceId])
      .subscribe(args => this.router.navigate(args));
  }

  createReview() {
    this.creating = true;
    Observable.of(<CreateReviewRequest>{size: this.numParticipants.value})
      .withLatestFrom(this.state.context$)
      .mergeMap(([request, context]) =>
        this.reviewAPI.createCohortReview(
          context.workspaceNamespace,
          context.workspaceId,
          context.cohortId,
          context.cdrVersion,
          request))
      .subscribe(review => {
        this.creating = false;
        this.state.review.next(review);
        this.open = false;
        this.openChange.emit(this.open);
      });
  }
}
