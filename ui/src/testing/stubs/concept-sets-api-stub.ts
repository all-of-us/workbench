
import {
  ConceptSet,
  ConceptSetListResponse,
  Domain
} from 'generated/fetch';
import {ConceptSetsApi, CreateConceptSetRequest, EmptyResponse} from 'generated/fetch/api';

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
      resolve({items: this.conceptSets});
    });
  }

  public updateConceptSet(
    workspaceNamespace: string, workspaceId: string, conceptSetId: number,
    req: ConceptSet): Promise<ConceptSet> {
    return new Promise<ConceptSet>(resolve => {
      const target = this.mustFindConceptSet(conceptSetId);
      target.name = req.name;
      target.description = req.description;
      resolve(target);
    });
  }

  public deleteConceptSet(
    workspaceNamespace: string, workspaceId: string,
    conceptSetId: number): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      const index = this.conceptSets.findIndex(cs => cs.id === conceptSetId);
      if (index < 0) {
        throw Error(`concept set ${conceptSetId} not found`);
      }
      this.conceptSets.splice(index, 1);
      resolve({});
    });
  }

  public createConceptSet(
    workspaceNamespace: string, workspaceId: string,
    conceptSetRequest: CreateConceptSetRequest): Promise<ConceptSet> {
    return new Promise<ConceptSet>(resolve => {
      this.conceptSets.push(conceptSetRequest.conceptSet);
      resolve(conceptSetRequest.conceptSet);
    });
  }

}
