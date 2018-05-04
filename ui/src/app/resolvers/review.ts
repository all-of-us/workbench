import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  PageFilterType,
  ParticipantCohortStatusColumns,
  ParticipantCohortStatuses,
  SortOrder,
  Workspace,
} from 'generated';

@Injectable()
export class ReviewResolver implements Resolve<CohortReview> {
  cdrId: number;

  constructor(
    private api: CohortReviewService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<CohortReview> {

    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);

    // console.log(`Resolving review for ${ns}/${wsid}:${cid}}`);
    // console.dir(route);
    /* Default values */
    const request = <ParticipantCohortStatuses>{
      page: 0,
      pageSize: 25,
      sortColumn: ParticipantCohortStatusColumns.ParticipantId,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
    };

    return this.workspaceStorageService.reloadIfNew(route.params['ns'], route.params['wsid'])
      .switchMap(() => this.workspaceStorageService.activeWorkspace$)
      .switchMap(() => this.api.getParticipantCohortStatuses(
        ns, wsid, cid, this.cdrId, request)).first();
  }
}
