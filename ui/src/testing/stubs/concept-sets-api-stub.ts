import {ConceptsServiceStub} from './concepts-service-stub';

import {
  ConceptSet,
  ConceptSetListResponse,
  Domain,
  UpdateConceptSetRequest
} from 'generated/fetch';
import {ConceptSetsApi, EmptyResponse} from 'generated/fetch/api';

export class ConceptSetsApiStub extends ConceptSetsApi {
  public conceptSets?: ConceptSet[];
  // TODO when this piece is converted
  // public conceptsStub?: ConceptsServiceStub;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });

    this.conceptSets = ConceptSetsApiStub.stubConceptSets();
    // this.conceptsStub = new ConceptsServiceStub();

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

  public getConceptSetsInWorkspace(
    workspaceNamespace: string, workspaceId: string): Promise<ConceptSetListResponse> {
    return new Promise<ConceptSetListResponse>(resolve => {
      setTimeout(() => {
        resolve({items: this.conceptSets});
      }, 0);
    });
  }

  public updateConceptSet(
    workspaceNamespace: string, workspaceId: string, conceptSetId: number,
    req: ConceptSet): Promise<ConceptSet> {
    return new Promise<ConceptSet>(resolve => {
      setTimeout(() => {
        const target = this.mustFindConceptSet(conceptSetId);
        target.name = req.name;
        target.description = req.description;
        console.log(target.name);
        resolve(target);
      }, 0);
    });
  }

  // public createConceptSet(
  //     workspaceNamespace: string, workspaceId: string, conceptSet?: ConceptSet,
  //     extraHttpRequestParams?: any): Promise<ConceptSet> {
  //   return new Promise<ConceptSet>(resolve => {
  //     setTimeout(() => {
  //       resolve(this.conceptSets[0]);
  //     });
  //   });
  // }

  // public updateConceptSetConcepts(
  //     workspaceNamespace: string, workspaceId: string, conceptSetId: number,
  //     req: UpdateConceptSetRequest): Observable<ConceptSet> {
  //   return new Observable<ConceptSet>(obs => {
  //     setTimeout(() => {
  //       const target = this.conceptSets.find(cs => cs.id === conceptSetId);
  //       if (!target) {
  //         throw Error(`concept set ${conceptSetId} not found`);
  //       }
  //       if (!target.concepts) {
  //         target.concepts = [];
  //       }
  //       for (const id of req.removedIds || []) {
  //         const index = target.concepts.findIndex(c => c.conceptId === id);
  //         if (index >= 0) {
  //           target.concepts.splice(index, 1);
  //         }
  //       }
  //       for (const id of req.addedIds || []) {
  //         const concept = this.conceptsStub.concepts.find(c => c.conceptId === id);
  //         if (!concept) {
  //           throw Error(`concept ${id} not found`);
  //         }
  //         target.concepts.push(concept);
  //       }
  //       obs.next(target);
  //       obs.complete();
  //     }, 0);
  //   });
  // }

  public deleteConceptSet(
    workspaceNamespace: string, workspaceId: string,
    conceptSetId: number): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      setTimeout(() => {
        const index = this.conceptSets.findIndex(cs => cs.id === conceptSetId);
        if (index < 0) {
          throw Error(`concept set ${conceptSetId} not found`);
        }
        this.conceptSets.splice(index, 1);
        resolve({});
      }, 0);
    });
  }
}
