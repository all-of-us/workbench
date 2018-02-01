import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {CohortReviewService} from 'generated';

/* TODO use a real CDR version */
const CDR_VERSION = 1;

@Injectable()
export class ParticipantResolver implements Resolve<Participant> {

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Participant> {
    const {ns, wsid, cid} = route.parent.params;
    const {pid} = route.params;

    // console.log(`Resolving participant at ${ns}/${wsid}, cohort ${cid} and pid ${pid}`);
    // console.dir(route);

    return <Observable<Participant>>this.reviewAPI
      .getParticipantCohortStatus(ns, wsid, +cid, CDR_VERSION, +pid)
      .map(Participant.makeRandomFromExisting)
      .do(participant => this.state.participant.next(participant));
  }
}
