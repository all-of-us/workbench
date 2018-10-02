import {Observable} from 'rxjs/Observable';

import {
  Concept,
  ConceptListResponse,
  Domain,
  DomainInfo,
  DomainInfoResponse,
  SearchConceptsRequest,
  StandardConceptFilter
} from 'generated';

export class ConceptStubVariables {
  static STUB_CONCEPTS: Concept[] = [
    {
      conceptId: 1,
      conceptName: 'Stub Concept 1',
      domainId: 'Conditions',
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
      domainId: 'Conditions',
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
      domainId: 'Measurements',
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
      domainId: 'Measurements',
      vocabularyId: 'SNOMED',
      conceptCode: 'G8107',
      conceptClassId: 'Ingredient',
      standardConcept: false,
      countValue: 1,
      prevalence: 1,
      conceptSynonyms: [
        'fake measure', 'very measure: wow'
      ]
    }
  ];
}

export class DomainStubVariables {
  static STUB_DOMAINS: DomainInfo[] = [
    {
      domain: Domain.CONDITION,
      name: 'Conditions',
      description: 'The Conditions Stub',
      standardConceptCount: 1,
      allConceptCount: 2,
      participantCount: 30
    },
    {
      domain: Domain.MEASUREMENT,
      name: 'Measurements',
      description: 'The Measurements Stub',
      standardConceptCount: 50,
      allConceptCount: 65,
      participantCount: 200
    }
  ];
}

export class ConceptsServiceStub {

  constructor() {}

  public getDomainInfo(
    workspaceNamespace: string, workspaceId: string): Observable<DomainInfoResponse> {
      return new Observable<DomainInfoResponse>(observer => {
        setTimeout(() => {
          observer.next({items: DomainStubVariables.STUB_DOMAINS});
          observer.complete();
        }, 0);
      });
  }

  // This just returns static values rather than doing a real search.
  // Real search functionality should be tested at the API level.
  // This creates more predictable responses.
  public searchConcepts(
    workspaceNamespace: string, workspaceId: string,
    request?: SearchConceptsRequest): Observable<ConceptListResponse> {
      return new Observable<ConceptListResponse>(observer => {
        setTimeout(() => {
          const response = {
            items: [],
            standardConcepts: [],
            vocabularyCounts: [],
            domainCounts: undefined
          };
          if (request.includeDomainCounts) {
            response.domainCounts = DomainStubVariables.STUB_DOMAINS.map((domainInfo) => {
              return {
                domain: domainInfo.domain,
                name: domainInfo.name,
                concept_count: domainInfo.allConceptCount
              };
            });
          }
          const foundDomain =
            DomainStubVariables.STUB_DOMAINS.find(domain => domain.domain === request.domain);
          ConceptStubVariables.STUB_CONCEPTS.forEach((concept) => {
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
          observer.next(response);
          observer.complete();
        }, 0);
      });
  }
}
