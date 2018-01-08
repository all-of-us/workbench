import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {Cohort, CohortsService} from 'generated';

@Injectable()
export class CohortResolver implements Resolve<Cohort> {

  constructor(
    private state: ReviewStateService,
    private cohortAPI: CohortsService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Cohort> {
    const ns = route.paramMap.get('ns');
    const wsid = route.paramMap.get('wsid');
    const cid = +route.paramMap.get('cid');

    console.log('Loading cohort from resolver');
    return this.cohortAPI
      .getCohort(ns, wsid, cid)
      .do(cohort => this.state.cohort.next(cohort));
  }
}
