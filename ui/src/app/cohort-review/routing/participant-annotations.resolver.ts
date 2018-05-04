import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {
  CohortReviewService,
  ParticipantCohortAnnotation,
  ParticipantCohortAnnotationListResponse,
} from 'generated';


@Injectable()
export class ParticipantAnnotationsResolver implements Resolve<ParticipantCohortAnnotation[]> {
  private cdrId: number;

  constructor(
    private reviewAPI: CohortReviewService,
    private workspaceStorageService: WorkspaceStorageService
  ) {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantCohortAnnotation[]> {
    const ns = route.parent.paramMap.get('ns');
    const wsid = route.parent.paramMap.get('wsid');
    const cid = +route.parent.paramMap.get('cid');

    const pid = +route.paramMap.get('pid');

    const empty = <ParticipantCohortAnnotationListResponse>{
      items: <ParticipantCohortAnnotation[]>[],
    };

    const rid = route.parent.data.review.cohortReviewId;

    // console.log(`Loading annotations from resolver for ${pid}`);
    // console.log(`ns: ${ns}, wsid: ${wsid}, cid: ${cid}`);
    // console.dir(route);

    const participantCohortAnnotationArray = new Subject<ParticipantCohortAnnotation[]>();
    this.workspaceStorageService.reloadIfNew(ns, wsid).subscribe(() => {
      // console.log(`Loading participant annotations`);
      // console.log(`cdr id: ${this.cdrId}`);
      // console.dir(route);

      (this.reviewAPI
        .getParticipantCohortAnnotations(ns, wsid, cid, this.cdrId, pid)
        .catch(err => Observable.of(empty))
        .pluck('items') as Observable<ParticipantCohortAnnotation[]>).subscribe((results) => {
        participantCohortAnnotationArray.next(results);
      });
    });
    return participantCohortAnnotationArray.asObservable().first();
  }
}
