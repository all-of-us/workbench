import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  Workspace,
} from 'generated';

/* TODO use a real CDR version */
const CDR_VERSION = 1;

@Injectable()
export class ReviewResolver implements Resolve<CohortReview> {

  constructor(private api: CohortReviewService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);

    // console.log(`Resolving review for ${ns}/${wsid}:${cid} @ CDR ${CDR_VERSION}`);
    // console.dir(route);

    return this.api.getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION);
  }
}
