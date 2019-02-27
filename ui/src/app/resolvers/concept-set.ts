import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import {
  ConceptSet,
  ConceptSetsService,
  Workspace
} from 'generated';
import {ConceptSet as FetchConceptSet} from 'generated/fetch';

import {currentConceptSetStore} from 'app/utils/navigation';

@Injectable()
export class ConceptSetResolver implements Resolve<ConceptSet> {
  constructor(
    private api: ConceptSetsService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Promise<ConceptSet> {
    const ns: Workspace['namespace'] = route.params.ns;
    const wsid: Workspace['id'] = route.params.wsid;
    const csid: ConceptSet['id'] = route.params.csid;

    return this.api.getConceptSet(ns, wsid, csid).do(v => {
      currentConceptSetStore.next(v as unknown as FetchConceptSet);
    }).toPromise();
  }
}
