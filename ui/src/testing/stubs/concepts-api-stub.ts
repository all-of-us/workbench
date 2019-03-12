import {Concept, Domain, DomainInfo, DomainInfoResponse} from 'generated/fetch';
import {ConceptsApi} from 'generated/fetch/api';

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

}
