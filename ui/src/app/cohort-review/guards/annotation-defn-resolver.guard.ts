import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

@Injectable()
export class AnnotationDefnResolver implements Resolve<CohortAnnotationDefinition[]> {

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortAnnotationDefinition[]> {
    const ns = route.paramMap.get('ns');
    const wsid = route.paramMap.get('wsid');
    const cid = +route.paramMap.get('cid');

    console.log('Loading annotation definitions from resolver');
    return this.annotationAPI
      .getCohortAnnotationDefinitions(ns, wsid, cid)
      .pluck('items')
      .do(defns => this.state.annotationDefinitions.next(<CohortAnnotationDefinition[]>defns));
  }
}
