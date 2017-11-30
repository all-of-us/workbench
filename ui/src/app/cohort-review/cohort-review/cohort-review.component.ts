// TODO(jms) - remove the linting silencer when this is beyond a stub
/* tslint:disable:no-unused-variable */
import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReviewService} from 'generated';

const CDR_VERSION = 1;

const subjects$ = Observable.of([
  {id: 1, status: 'NR'},
  {id: 2, status: 'NR'},
  {id: 3, status: 'NR'},
]);

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent {
  private loading: boolean;
  private collapsed = false;

  private subjects$ = subjects$;
  private activeSubject$;

  constructor(
    route: ActivatedRoute,
    reviewApi: CohortReviewService,
  ) {}
}
