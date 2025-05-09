import {
  ConceptSet,
  ConceptSetListResponse,
  ConceptSetsApi,
  CreateConceptSetRequest,
  Criteria,
  CriteriaType,
  Domain,
  EmptyResponse,
  Surveys,
  UpdateConceptSetRequest,
} from 'generated/fetch';

export class ConceptStubVariables {
  static STUB_CONCEPTS: Criteria[] = [
    {
      id: 1,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 1',
      domainId: Domain.CONDITION.toString(),
      type: CriteriaType.SNOMED.toString(),
      conceptId: 8107,
      standard: true,
      childCount: 1,
      parentCount: 0,
    },
    {
      id: 2,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 2',
      domainId: Domain.CONDITION.toString(),
      type: CriteriaType.SNOMED.toString(),
      conceptId: 8107,
      standard: true,
      childCount: 2,
      parentCount: 0,
    },
    {
      id: 3,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 3',
      domainId: Domain.MEASUREMENT.toString(),
      type: CriteriaType.LOINC.toString(),
      conceptId: 1234,
      standard: true,
      childCount: 1,
      parentCount: 0,
    },
    {
      id: 4,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 4',
      domainId: Domain.MEASUREMENT.toString(),
      type: CriteriaType.LOINC.toString(),
      conceptId: 2345,
      standard: true,
      childCount: 1,
      parentCount: 0,
    },
  ];
}

export class ConceptSetsApiStub extends ConceptSetsApi {
  public conceptSets: ConceptSet[];
  public surveyConceptSets: ConceptSet[];
  // TODO when this piece is converted

  constructor() {
    super(undefined);

    this.conceptSets = ConceptSetsApiStub.stubConceptSets();
    this.surveyConceptSets = ConceptSetsApiStub.stubConceptSets().filter(
      (concepts) => {
        return concepts.domain === Domain.OBSERVATION;
      }
    );
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
        criteriums: ConceptStubVariables.STUB_CONCEPTS,
      },
      {
        id: 346,
        name: 'Mock Concept Set Measurement',
        description: 'Mocked for tests',
        domain: Domain.MEASUREMENT,
        lastModifiedTime: new Date().getTime(),
        criteriums: [],
      },
      {
        id: 347,
        name: 'Mock Concept Set for condition',
        description: 'Mocked for tests',
        domain: Domain.CONDITION,
        lastModifiedTime: new Date().getTime() - 2000,
        criteriums: [],
      },
      {
        id: 348,
        name: 'Mock Concept Set for survey overAll health',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.OVERALL_HEALTH,
        lastModifiedTime: new Date().getTime() - 2000,
        criteriums: [],
      },
      {
        id: 349,
        name: 'Mock Concept Set for survey Lifestyle',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.LIFESTYLE,
        lastModifiedTime: new Date().getTime() - 2000,
        criteriums: [],
      },
      {
        id: 350,
        name: 'Mock Concept Set for survey Basic health',
        description: 'Mocked for tests',
        domain: Domain.OBSERVATION,
        survey: Surveys.LIFESTYLE,
        lastModifiedTime: new Date().getTime() - 2000,
        criteriums: [],
      },
      {
        id: 351,
        name: 'Mock WGS',
        description: 'Mocked for tests',
        domain: Domain.WHOLE_GENOME_VARIANT,
        lastModifiedTime: new Date().getTime() - 2000,
        criteriums: [],
      },
    ];
  }

  private mustFindConceptSet(conceptSetId: number): ConceptSet {
    const target = this.conceptSets.find((cs) => cs.id === conceptSetId);
    if (!target) {
      throw Error(`concept set ${conceptSetId} not found`);
    }
    return target;
  }

  public getConceptSetsInWorkspace(): Promise<ConceptSetListResponse> {
    return new Promise<ConceptSetListResponse>((resolve) => {
      resolve({ items: this.conceptSets });
    });
  }

  public getConceptSet(
    _ns: string,
    _terraName: string,
    conceptSetId: number
  ): Promise<ConceptSet> {
    return new Promise<ConceptSet>((resolve) => {
      resolve(this.mustFindConceptSet(conceptSetId));
    });
  }

  public updateConceptSet(
    _ns: string,
    _terraName: string,
    conceptSetId: number,
    req: ConceptSet
  ): Promise<ConceptSet> {
    return new Promise<ConceptSet>((resolve) => {
      const target = this.mustFindConceptSet(conceptSetId);
      target.name = req.name;
      target.description = req.description;
      resolve(target);
    });
  }

  public deleteConceptSet(
    _ns: string,
    _terraName: string,
    conceptSetId: number
  ): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      const index = this.conceptSets.findIndex((cs) => cs.id === conceptSetId);
      if (index < 0) {
        throw Error(`concept set ${conceptSetId} not found`);
      }
      this.conceptSets.splice(index, 1);
      resolve({});
    });
  }

  public createConceptSet(
    _ns: string,
    _terraName: string,
    conceptSetRequest: CreateConceptSetRequest
  ): Promise<ConceptSet> {
    return new Promise<ConceptSet>((resolve) => {
      this.conceptSets.push(conceptSetRequest.conceptSet);
      resolve(conceptSetRequest.conceptSet);
    });
  }

  public updateConceptSetConcepts(
    _ns: string,
    _terraName: string,
    conceptSetId: number,
    req: UpdateConceptSetRequest
  ): Promise<ConceptSet> {
    return new Promise<ConceptSet>((resolve) => {
      const target = this.conceptSets.find((cs) => cs.id === conceptSetId);
      if (!target) {
        throw Error(`concept set ${conceptSetId} not found`);
      }
      if (!target.criteriums) {
        target.criteriums = [];
      }
      for (const id of req.removedConceptSetConceptIds || []) {
        const index = target.criteriums.findIndex(
          (c) => c.conceptId === id.conceptId
        );
        if (index >= 0) {
          target.criteriums = target.criteriums.filter(
            (concept) => concept.conceptId !== id.conceptId
          );
        }
      }
      for (const id of req.addedConceptSetConceptIds || []) {
        const concept = ConceptStubVariables.STUB_CONCEPTS.find(
          (c) => c.conceptId === id.conceptId
        );
        if (!concept) {
          throw Error(`concept ${id} not found`);
        }
        target.criteriums.push(concept);
      }
      resolve(target);
    });
  }

  public countConceptsInConceptSet(): Promise<number> {
    return new Promise<number>((resolve) => {
      resolve(this.conceptSets[0].criteriums.length);
    });
  }

  copyConceptSet(): Promise<any> {
    return new Promise<any>((resolve) => {
      resolve({});
    });
  }
}
