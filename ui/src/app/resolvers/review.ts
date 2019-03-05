import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {
  Cohort,
  CohortReview,
  PageFilterType,
  ParticipantCohortStatusColumns,
  ParticipantCohortStatuses,
  SortOrder,
  Workspace,
} from 'generated/fetch';

@Injectable()
export class ReviewResolver implements Resolve<CohortReview> {

  constructor() {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);
    const cdrid = route.parent.data.workspace.cdrVersionId;

    // console.log(`Resolving review for ${ns}/${wsid}:${cid} @ cdr id ${cdrid}`);
    // console.dir(route);

    /* Default values */
    const request = <ParticipantCohortStatuses>{
      page: 0,
      pageSize: 25,
      sortColumn: ParticipantCohortStatusColumns.PARTICIPANTID,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
    };
    const observable = from(cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, cdrid, request))
    return observable.map(v => {
      cohortReviewStore.next(v);
      return v;
    });
  }
}
