import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {currentCohortStore} from 'app/utils/navigation';
import {Cohort, CohortsService, Workspace} from 'generated';

@Injectable()
export class CohortResolver implements Resolve<Cohort> {

  constructor(private api: CohortsService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Cohort> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const cid: Cohort['id'] = +(route.params.cid);

    // console.log(`Resolving cohort ${cid}:`);
    // console.dir(route);

    return this.api.getCohort(ns, wsid, cid).map(c => {
      currentCohortStore.next(c);
      return c;
    });
  }
}
