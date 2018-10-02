import {Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {
  ConceptSet,
  ConceptSetListResponse,
  Domain
} from 'generated';

export class ConceptSetsServiceStub {

  conceptList: ConceptSet[];

  constructor() {
    this.conceptList = ConceptSetsServiceStub.stubConceptList();
  }

  static stubConceptList(): ConceptSet[] {
    return [
      {
        'name': 'Mock Concept Set',
        'description': 'Mocked for tests',
        'domain': Domain.CONDITION
      }
    ];
  }

  public getConceptSetsInWorkspace(
    workspaceNamespace: string, workspaceId: string): Observable<ConceptSetListResponse> {
      return new Observable<ConceptSetListResponse>(observer => {
        setTimeout(() => {
          observer.next({items: this.conceptList});
          observer.complete();
        }, 0);
      });
  }

  public updateConceptSet(workspaceNamespace: string, workspaceId: string, conceptId: number,
                          conceptSet: ConceptSet): Observable<ConceptSet> {
    return new Observable<ConceptSet>(observer => {
      setTimeout(() => {
        observer.next([conceptSet]);
        this.conceptList = [conceptSet];
        observer.complete();
      }, 0);
    });
  }

  public deleteConceptSet(workspaceNamespace: string, workspaceId: string, conceptId: number):
  Observable<void> {
    return new Observable<void>(observer => {
      setTimeout(() => {
        observer.next();
        this.conceptList = [];
        observer.complete();
      });
    });
  }
}
