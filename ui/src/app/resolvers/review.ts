import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatusColumns,
  ParticipantCohortStatusesRequest,
  SortOrder,
  Workspace,
} from 'generated';

@Injectable()
export class ReviewResolver implements Resolve<CohortReview> {

  constructor(private api: CohortReviewService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);
    const cdrid = route.parent.data.workspace.cdrVersionId;

    // console.log(`Resolving review for ${ns}/${wsid}:${cid} @ cdr id ${cdrid}`);
    // console.dir(route);

    /* Default values */
    const request = <ParticipantCohortStatusesRequest>{
      page: 0,
      pageSize: 25,
      sortColumn: ParticipantCohortStatusColumns.ParticipantId,
      sortOrder: SortOrder.Asc,
    };

    return this.api.getParticipantCohortStatuses(ns, wsid, cid, cdrid, request);
  }
}
