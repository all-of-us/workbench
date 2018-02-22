import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {CohortReviewService} from 'generated';

@Injectable()
export class ParticipantResolver implements Resolve<Participant> {

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Participant> {
    const {ns, wsid, cid} = route.parent.params;
    const cdrid = route.parent.data.workspace.cdrVersionId;
    const {pid} = route.params;

    // console.log(`Resolving participant at ${ns}/${wsid}, cohort ${cid} and pid ${pid}`);
    // console.dir(route);

    return <Observable<Participant>>this.reviewAPI
      .getParticipantCohortStatus(ns, wsid, +cid, cdrid, +pid)
      .map(Participant.fromStatus)
      .do(participant => this.state.participant.next(participant));
  }
}
