import {Observable} from 'rxjs/Observable';

import {ConceptsServiceStub} from './concepts-service-stub';

import {
  ConceptSet,
  ConceptSetListResponse,
  Domain,
  UpdateConceptSetRequest
} from 'generated';

export class ConceptSetsServiceStub {

  constructor(
    public conceptSets?: ConceptSet[],
    public conceptsStub?: ConceptsServiceStub
  ) {
    if (!this.conceptSets) {
      this.conceptSets = ConceptSetsServiceStub.stubConceptSets();
    }
    if (!conceptsStub) {
      this.conceptsStub = new ConceptsServiceStub();
    }
  }

  static stubConceptSets(): ConceptSet[] {
    return [
      {
        id: 345,
        name: 'Mock Concept Set',
        description: 'Mocked for tests',
        domain: Domain.CONDITION,
        lastModifiedTime: new Date().getTime() - 8000
      },  {
        id: 346,
        name: 'Mock Concept Set Measurement',
        description: 'Mocked for tests',
        domain: Domain.MEASUREMENT,
        lastModifiedTime: new Date().getTime()
      },  {
        id: 347,
        name: 'Mock Concept Set for condition',
        description: 'Mocked for tests',
        domain: Domain.CONDITION,
        lastModifiedTime: new Date().getTime() - 2000
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

  public getConceptSet(ns, wsid, csid) {
    return Observable.of(this.mustFindConceptSet(csid));
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

  public updateConceptSetConcepts(
    workspaceNamespace: string, workspaceId: string, conceptSetId: number,
    req: UpdateConceptSetRequest): Observable<ConceptSet> {
    return new Observable<ConceptSet>(obs => {
      setTimeout(() => {
        const target = this.conceptSets.find(cs => cs.id === conceptSetId);
        if (!target) {
          throw Error(`concept set ${conceptSetId} not found`);
        }
        if (!target.concepts) {
          target.concepts = [];
        }
        for (const id of req.removedIds || []) {
          const index = target.concepts.findIndex(c => c.conceptId === id);
          if (index >= 0) {
            target.concepts.splice(index, 1);
          }
        }
        for (const id of req.addedIds || []) {
          const concept = this.conceptsStub.concepts.find(c => c.conceptId === id);
          if (!concept) {
            throw Error(`concept ${id} not found`);
          }
          target.concepts.push(concept);
        }
        obs.next(target);
        obs.complete();
      }, 0);
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
