import {Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {
  ConceptSet,
  ConceptSetListResponse,
  Domain
} from 'generated';

export class ConceptSetsServiceStub {

  constructor(public conceptSets?: ConceptSet[]) {
    if (!this.conceptSets) {
      this.conceptSets = ConceptSetsServiceStub.stubConceptSets();
    }
  }

  static stubConceptSets(): ConceptSet[] {
    return [
      {
        id: 345,
        name: 'Mock Concept Set',
        description: 'Mocked for tests',
        domain: Domain.CONDITION
      }
    ];
  }

  private mustFindConceptSet(conceptSetId: number): ConceptSet {
    const target = this.conceptSets.find(cs => cs.id === conceptSetId);
    if (!target) {
      throw Error(`concept set ${conceptSetId} not found`);
    }
    return target;
  }

  public getConceptSetsInWorkspace(
    workspaceNamespace: string, workspaceId: string): Observable<ConceptSetListResponse> {
      return new Observable<ConceptSetListResponse>(observer => {
        setTimeout(() => {
          observer.next({items: this.conceptSets});
          observer.complete();
        }, 0);
      });
    }

  public updateConceptSet(
      workspaceNamespace: string, workspaceId: string, conceptSetId: number,
      req: ConceptSet): Observable<ConceptSet> {
    return new Observable<ConceptSet>(obs => {
      setTimeout(() => {
        const target = this.mustFindConceptSet(conceptSetId);
        target.name = req.name;
        target.description = req.description;
        obs.next(target);
        obs.complete();
      }, 0);
    });
  }

  public createConceptSet(workspaceNamespace: string, workspaceId: string, conceptSet?: ConceptSet,
                          extraHttpRequestParams?: any): Observable<ConceptSet> {
    return new Observable<ConceptSet>(observer => {
      setTimeout(() => {
        observer.next(this.conceptSets[0]);
        observer.complete();
      });
    });
  }

  public deleteConceptSet(
      workspaceNamespace: string, workspaceId: string,
      conceptSetId: number): Observable<void> {
    return new Observable<void>(obs => {
      setTimeout(() => {
        const index = this.conceptSets.findIndex(cs => cs.id === conceptSetId);
        if (index < 0) {
          throw Error(`concept set ${conceptSetId} not found`);
        }
        this.conceptSets.splice(index, 1);
        obs.next();
        obs.complete();
      }, 0);
    });
  }
}
