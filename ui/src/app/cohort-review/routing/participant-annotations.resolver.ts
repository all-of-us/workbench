import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  ParticipantCohortAnnotation,
  ParticipantCohortAnnotationListResponse,
} from 'generated';

@Injectable()
export class ParticipantAnnotationsResolver implements Resolve<ParticipantCohortAnnotation[]> {

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantCohortAnnotation[]> {
    const ns = route.parent.paramMap.get('ns');
    const wsid = route.parent.paramMap.get('wsid');
    const cid = +route.parent.paramMap.get('cid');
    const cdrid = +route.parent.data.workspace.cdrVersionId;

    const pid = +route.paramMap.get('pid');

    const empty = <ParticipantCohortAnnotationListResponse>{
      items: <ParticipantCohortAnnotation[]>[],
    };

    const rid = route.parent.data.review.cohortReviewId;

    // console.log(`Loading annotations from resolver for ${pid}`);
    // console.log(`ns: ${ns}, wsid: ${wsid}, cid: ${cid}, cdrid: ${cdrid}`);
    // console.dir(route);

    const call = this.reviewAPI
      .getParticipantCohortAnnotations(ns, wsid, cid, cdrid, pid)
      .catch(err => Observable.of(empty))
      .pluck('items')
      .do(vals => this.state.annotationValues.next(<ParticipantCohortAnnotation[]>vals));

    return (call as Observable<ParticipantCohortAnnotation[]>);
  }
}
