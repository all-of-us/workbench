
import {UpdateConceptSetRequest} from 'generated';
import {
  ConceptSet,
  ConceptSetListResponse,
  Domain,
  Surveys
} from 'generated/fetch';

import {ConceptSetsApi, CreateConceptSetRequest, EmptyResponse} from 'generated/fetch/api';
import {ConceptsApiStub, ConceptStubVariables} from './concepts-api-stub';

export class ConceptSetsApiStub extends ConceptSetsApi {
  public conceptSets: ConceptSet[];
  public surveyConceptSets: ConceptSet[];
  // TODO when this piece is converted
  public conceptsStub?: ConceptsApiStub;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });

    this.conceptSets = ConceptSetsApiStub.stubConceptSets();
    this.conceptsStub = new ConceptsApiStub();
    this.surveyConceptSets  =  ConceptSetsApiStub.stubConceptSets().filter((concepts) => {
      return concepts.domain === Domain.OBSERVATION;
    });
  }

  static stubConceptSets(): ConceptSet[] {
    return [
      {
        id: 345,
        name: 'Mock Concept Set',
        description: 'Mocked for tests',
        domain: Domain.CONDITION,
        lastModifiedTime: new Date().getTime() - 8000,
        participantCount: ConceptStubVariables.STUB_CONCEPTS.length,
        concepts: ConceptStubVariables.STUB_CONCEPTS
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
      }, {
        id: 348,
        name: 'Mock Concept Set for survey overAll health',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.OVERALLHEALTH,
        lastModifiedTime: new Date().getTime() - 2000
      }, {
        id: 349,
        name: 'Mock Concept Set for survey Lifestyle',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.LIFESTYLE,
        lastModifiedTime: new Date().getTime() - 2000
      }, {
        id: 350,
        name: 'Mock Concept Set for survey Basic health',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.LIFESTYLE,
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

  public getSurveyConceptSetsInWorkspace(
    workspaceNamespace: string, workspaceId: string, surveyName: string):
  Promise<ConceptSetListResponse> {
    return new Promise<ConceptSetListResponse>(resolve => {
      resolve({items: this.surveyConceptSets.filter((survey) => {
        return survey.survey.toString() === surveyName;
      })});
    });
  }

  public getConceptSet(workspaceNamespace: string, workspaceId: string, conceptSetId: number):
  Promise<ConceptSet> {
    return new Promise<ConceptSet>(resolve => {
      resolve(this.mustFindConceptSet(conceptSetId));
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

  public updateConceptSetConcepts(
    workspaceNamespace: string, workspaceId: string, conceptSetId: number,
    req: UpdateConceptSetRequest): Promise<ConceptSet> {
    return new Promise<ConceptSet>(resolve => {
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
          target.concepts = target.concepts.filter(concept => concept.conceptId !== id);
        }
      }
      for (const id of req.addedIds || []) {
        const concept = this.conceptsStub.concepts.find(c => c.conceptId === id);
        if (!concept) {
          throw Error(`concept ${id} not found`);
        }
        target.concepts.push(concept);
      }
      resolve(target);
    });
  }

}
