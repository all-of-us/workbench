import {Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {
  ConceptSetListResponse
} from 'generated';

export class ConceptSetsServiceStub {

  constructor() {}

  public getConceptSetsInWorkspace(
    workspaceNamespace: string, workspaceId: string): Observable<ConceptSetListResponse> {
      return new Observable<ConceptSetListResponse>(observer => {
        setTimeout(() => {
          observer.next({items: []});
          observer.complete();
        }, 0);
      });
  }
}
