import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {CohortReview, CohortReviewService} from 'generated';

/* TODO use a real CDR version */
const CDR_VERSION = 1;

@Injectable()
export class ReviewResolver implements Resolve<CohortReview> {

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {
    const ns = route.paramMap.get('ns');
    const wsid = route.paramMap.get('wsid');
    const cid = +route.paramMap.get('cid');

    console.log('Loading review from resolver');
    return this.reviewAPI
      .getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION)
      .do(review => this.state.review.next(review));
  }
}
