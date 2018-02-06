import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  FilterList,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatusesRequest as Request,
  SortOrder,
  Workspace,
} from 'generated';

/* TODO use a real CDR version */
const CDR_VERSION = 1;

export const DEFAULT_PAGE = 0;
export const DEFAULT_PAGE_SIZE = 25;
export const DEFAULT_SORT_COLUMN = Columns.ParticipantId;
export const DEFAULT_SORT_ORDER = SortOrder.Asc;
export const DEFAULT_FILTER_SET = [];

@Injectable()
export class ParticipantPageResolver implements Resolve<ParticipantCohortStatus[]> {

  constructor(private api: CohortReviewService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantCohortStatus[]> {
    const params: any = route.pathFromRoot.reduce((p, r) => ({...p, ...r.params}), {});
    const query: any = route.pathFromRoot.reduce((p, r) => ({...p, ...r.queryParams}), {});

    const ns: Workspace['namespace'] = params.ns;
    const wsid: Workspace['id'] = params.wsid;
    const cid: Cohort['id'] = +(params.cid);

    /* Can't use just 'or', zero is a legitimate page number */
    const page = query.page === undefined ? DEFAULT_PAGE : (query.page - 1);
    /* Zero is not a legitimate page size though - none of these can take falsey values */
    const pageSize = query.pageSize || DEFAULT_PAGE_SIZE;
    const sortColumn = query.sortColumn || DEFAULT_SORT_COLUMN;
    const sortOrder = query.sortOrder || DEFAULT_SORT_ORDER;

    const filters = <FilterList>{
      items: query.filters || DEFAULT_FILTER_SET,
    };

    // console.log(`Resolving statuses for ${ns}/${wsid}:${cid} @ CDR ${CDR_VERSION}`);
    // console.dir(route);

    const request = <Request>{page, pageSize, sortColumn, sortOrder, filters};

    return this.api
      .getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION, request)
      .pluck<CohortReview, ParticipantCohortStatus[]>('participantCohortStatuses');
  }
}
