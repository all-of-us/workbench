import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {Cohort, CohortsService} from 'generated';

@Injectable()
export class CohortResolver implements Resolve<Cohort> {

  constructor(private cohortAPI: CohortsService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Cohort> {
    const wsNamespace = route.paramMap.get('ns');
    const wsID = route.paramMap.get('wsid');
    const cohortID = +route.paramMap.get('cid');

    return this.cohortAPI.getCohort(wsNamespace, wsID, cohortID);
  }
}
