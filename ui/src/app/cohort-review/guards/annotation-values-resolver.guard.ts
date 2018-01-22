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
export class AnnotationValuesResolver implements Resolve<ParticipantCohortAnnotation[]> {

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantCohortAnnotation[]> {
    const ns = route.parent.paramMap.get('ns');
    const wsid = route.parent.paramMap.get('wsid');
    const cid = +route.parent.paramMap.get('cid');
    const pid = +route.paramMap.get('pid');

    // TODO (jms) This is a temporary fix while backend is being implemented
    const empty = <ParticipantCohortAnnotationListResponse>{
      items: <ParticipantCohortAnnotation[]>[],
    };

    console.log(`Loading annotations from resolver for ${pid}`);
    return this.reviewAPI
      .getParticipantCohortAnnotations(ns, wsid, cid, pid)
      .catch(err => Observable.of(empty))
      .pluck('items')
      .do(vals => this.state.annotationValues.next(<ParticipantCohortAnnotation[]>vals));
  }
}
