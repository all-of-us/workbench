import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {annotationDefinitionsStore} from 'app/cohort-review/review-state.service';
import {
  Cohort,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  Workspace,
} from 'generated';

@Injectable()
export class AnnotationDefinitionsResolver implements Resolve<CohortAnnotationDefinition[]> {

  constructor(private api: CohortAnnotationDefinitionService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<CohortAnnotationDefinition[]> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);

    // console.log(`Resolving annotation definitions for ${ns}/${wsid}:${cid}`);

    return this.api.getCohortAnnotationDefinitions(ns, wsid, cid).map(({items}) => items).do(v => {
      annotationDefinitionsStore.next(v);
    });
  }
}
