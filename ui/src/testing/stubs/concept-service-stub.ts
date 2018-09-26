import {Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {
  ConceptListResponse,
  DomainInfoResponse,
  SearchConceptsRequest
} from 'generated';

export class ConceptsServiceStub {

  constructor() {}

  public getDomainInfo(
    workspaceNamespace: string, workspaceId: string): Observable<DomainInfoResponse> {
      return new Observable<DomainInfoResponse>(observer => {
        setTimeout(() => {
          observer.next({items: []});
          observer.complete();
        }, 0);
      });
  }

  public searchConcepts(
    workspaceNamespace: string, workspaceId: string,
    request?: SearchConceptsRequest): Observable<ConceptListResponse> {
      return new Observable<ConceptListResponse>(observer => {
        setTimeout(() => {
          observer.next({items: []});
          observer.complete();
        }, 0);
      });
  }
}
