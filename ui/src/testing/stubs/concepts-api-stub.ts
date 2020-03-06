import {
  Concept,
  ConceptListResponse,
  ConceptsApi,
  Domain,
  DomainCount,
  DomainCountsListResponse,
  DomainInfo,
  DomainInfoResponse,
  SearchConceptsRequest,
  StandardConceptFilter,
  SurveyModule, SurveyQuestions,
  SurveysResponse
} from 'generated/fetch';

export class ConceptStubVariables {
  static STUB_CONCEPTS: Concept[] = [
    {
      conceptId: 1,
      conceptName: 'Stub Concept 1',
      domainId: 'Condition',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: true,
      countValue: 1,
      prevalence: 1,
      conceptSynonyms: [
        'blah', 'blahblah'
      ]
    },
    {
      conceptId: 2,
      conceptName: 'Stub Concept 2',
      domainId: 'Condition',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: false,
      countValue: 2,
      prevalence: 2,
      conceptSynonyms: [
        'merp', 'kerp'
      ]
    },
    {
      conceptId: 3,
      conceptName: 'Stub Concept 3',
      domainId: 'Measurement',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: true,
      countValue: 1,
      prevalence: 1,
      conceptSynonyms: [
        'measure', 'super measure'
      ]
    },
    {
      conceptId: 4,
      conceptName: 'Stub Concept 4',
      domainId: 'Measurement',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: false,
      countValue: 1,
      prevalence: 1,
      conceptSynonyms: [
        'fake measure', 'very measure: wow'
      ]
    },
    {
      conceptId: 5,
      conceptName: 'Stub Survey Question Concept 1',
      domainId: 'Observation',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: true,
      countValue: 1,
      prevalence: 1,
      conceptSynonyms: [
        'blah', 'blahblah'
      ]
    }
  ];
}
export class SurveyStubVariables {
  static STUB_SURVEYS: SurveyModule[] = [
    {
      conceptId: 1,
      name: 'The Basics',
      description: 'Basis description',
      questionCount: 101,
      participantCount: 200,
      orderNumber: 1
    },
    {
      conceptId: 2,
      name: 'Overall Health',
      description: 'Overall Health description',
      questionCount: 102,
      participantCount: 300,
      orderNumber: 2
    },
    {
      conceptId: 3,
      name: 'LifeStyle',
      description: 'Lifestyle description',
      questionCount: 103,
      participantCount: 300,
      orderNumber: 3
    }
  ];
}

export class DomainStubVariables {
  static STUB_DOMAINS: DomainInfo[] = [
    {
      domain: Domain.CONDITION,
      name: 'Condition',
      description: 'The Conditions Stub',
      standardConceptCount: 1,
      allConceptCount: 2,
      participantCount: 30
    },
    {
      domain: Domain.MEASUREMENT,
      name: 'Measurement',
      description: 'The Measurements Stub',
      standardConceptCount: 50,
      allConceptCount: 65,
      participantCount: 200
    },
  ];
}

export class DomainCountStubVariables {
  static STUB_DOMAIN_COUNTS: DomainCount[] = [
    {
      domain: Domain.CONDITION,
      name: 'Condition',
      conceptCount: 2
    }, {
      domain: Domain.MEASUREMENT,
      name: 'Measurement',
      conceptCount: 1
    }, {
      domain: Domain.DRUG,
      name: 'Drug',
      conceptCount: 2
    }
  ];
}

export class SurveyQuestionStubVariables {
  static STUB_SURVEY_QUESTIONS: SurveyQuestions[] = [
    {
      question: 'Survey question 1',
      conceptId: 1
    }, {
      question: 'Survey question 2',
      conceptId: 2
    }
  ];
}

export class ConceptsApiStub extends ConceptsApi {
  public concepts?: Concept[];
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });

    this.concepts = ConceptStubVariables.STUB_CONCEPTS;
  }

  public getDomainInfo(
    workspaceNamespace: string, workspaceId: string): Promise<DomainInfoResponse> {
    return Promise.resolve({items: DomainStubVariables.STUB_DOMAINS});
  }

  public getSurveyInfo(workspaceNamespace: string, workspaceId: string): Promise<SurveysResponse> {
    return Promise.resolve({items: SurveyStubVariables.STUB_SURVEYS});
  }

  public domainCounts(workspaceNamespace: string, workspaceId: string): Promise<DomainCountsListResponse> {
    return Promise.resolve({domainCounts: DomainCountStubVariables.STUB_DOMAIN_COUNTS});
  }

  // This just returns static values rather than doing a real search.
  // Real search functionality should be tested at the API level.
  // This creates more predictable responses.
  public searchConcepts(
    workspaceNamespace: string, workspaceId: string,
    request?: SearchConceptsRequest): Promise<ConceptListResponse> {
    return new Promise<ConceptListResponse>(resolve => {
      const response = {
        items: [],
        standardConcepts: [],
        vocabularyCounts: [],
      };
      const foundDomain =
        DomainStubVariables.STUB_DOMAINS.find(domain => domain.domain === request.domain);
      if (request.query === 'headerText') {
        this.extendConceptListForHeaderText();
      }
      this.concepts.forEach((concept) => {
        if (concept.domainId !== foundDomain.name) {
          return;
        }
        if (request.standardConceptFilter === StandardConceptFilter.ALLCONCEPTS) {
          response.items.push(concept);
          if (concept.standardConcept) {
            response.standardConcepts.push(concept);
          }
        } else if (
          request.standardConceptFilter === StandardConceptFilter.STANDARDCONCEPTS) {
          if (concept.standardConcept) {
            response.items.push(concept);
            response.standardConcepts.push(concept);
          }
        } else if (request.standardConceptFilter
          === StandardConceptFilter.NONSTANDARDCONCEPTS) {
          if (!concept.standardConcept) {
            response.items.push(concept);
          }
        }
      });
      resolve(response);
    });
  }

  private extendConceptListForHeaderText() {
    // Starting with id as 6 to continue with this.concepts
    for (let i = 6; i < 46; i++) {
      const concept = {
        conceptId: i,
        conceptName: 'Stub Concept ' + i,
        domainId: 'Condition',
        vocabularyId: 'SNOMED',
        conceptCode: 'G8107',
        conceptClassId: 'Ingredient',
        standardConcept: true,
        countValue: 1,
        prevalence: 1,
        conceptSynonyms: [
          'blah', 'blahblah'
        ]
      };
      this.concepts = this.concepts.concat(concept);
    }
  }

  public searchSurveys(workspaceNamespace: string, workspaceId: string, request?: SearchConceptsRequest): Promise<Array<SurveyQuestions>> {
    return Promise.resolve(SurveyQuestionStubVariables.STUB_SURVEY_QUESTIONS);
  }

}
