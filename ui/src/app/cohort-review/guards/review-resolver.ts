import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReview, CohortReviewService} from 'generated';

@Injectable()
export class ReviewResolverGuard implements Resolve<CohortReview> {

  constructor(private reviewAPI: CohortReviewService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {
    const wsNamespace = route.paramMap.get('ns');
    const wsID = route.paramMap.get('wsid');
    const cohortID = +route.paramMap.get('cid');

    /* TODO use a real CDR version */
    const CDR_VERSION = 1;

    return this.reviewAPI.getParticipantCohortStatuses(wsNamespace, wsID, cohortID, CDR_VERSION);
  }
}
