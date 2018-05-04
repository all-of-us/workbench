import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';

import {Participant} from '../participant.model';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {CohortReviewService} from 'generated';

@Injectable()
export class ParticipantResolver implements Resolve<Participant> {
  cdrId: number;

  constructor(
    private reviewAPI: CohortReviewService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Participant> {
    const {ns, wsid, cid} = route.parent.params;
    const {pid} = route.params;


    const participant = new Subject<Participant>();
    this.workspaceStorageService.reloadIfNew(
      route.parent.params['ns'],
      route.parent.params['wsid']).subscribe(() => {
      // console.log(`Loading participant cohort status`);
      // console.log(`cdr id: ${this.cdrId}`);
      // console.dir(route);

      (<Observable<Participant>>this.reviewAPI
        .getParticipantCohortStatus(ns, wsid, +cid, this.cdrId, +pid)
        .map(Participant.fromStatus)).subscribe((result) => {
        participant.next(result);
      });
    });
    return participant.asObservable().first();
  }
}
