import {
  CohortBuilderApi,
  Criteria,
  CriteriaAttributeListResponse,
  CriteriaListResponse, CriteriaMenuOptionsListResponse,
  CriteriaType,
  DemoChartInfoListResponse,
  Domain,
  DomainCount,
  DomainInfo,
  DomainInfoResponse,
  ParticipantDemographics,
  SurveyCount,
  SurveyModule,
  SurveysResponse
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

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

export const cohortStub = {
  name: 'Test Cohort',
  criteria: '{}',
  type: '',
};

const criteriaStub = {
  id: 1,
  parentId: 0,
  type: CriteriaType[CriteriaType.ICD9CM],
  subtype: '',
  code: '123',
  name: 'Test',
  count: 1,
  group: false,
  selectable: true,
  conceptId: 123,
  domainId: Domain[Domain.CONDITION],
  hasAttributes: false,
  path: '0',
};

const domainCountStub = {
  domain: Domain.CONDITION,
  name: Domain.CONDITION.toString(),
  conceptCount: 1
};

const surveyCountStub = {
  name: 'The Basics',
  conceptCount: 1
};

export class CohortBuilderServiceStub extends CohortBuilderApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  findDemoChartInfo(): Promise<DemoChartInfoListResponse> {
    return new Promise<DemoChartInfoListResponse>(resolve => resolve({items: []}));
  }

  countParticipants(): Promise<number> {
    return new Promise<number>(resolve => resolve(1));
  }

  findCriteriaAttributeByConceptId(): Promise<CriteriaAttributeListResponse> {
    return new Promise<CriteriaAttributeListResponse>(resolve => resolve({items: []}));
  }

  findCriteriaAutoComplete(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  findCriteriaBy(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  findDrugBrandOrIngredientByValue(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  findDrugIngredientByConceptId(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  getPPICriteriaParent(): Promise<Criteria> {
    return new Promise<Criteria>(resolve => resolve(criteriaStub));
  }

  findParticipantDemographics(): Promise<ParticipantDemographics> {
    return new Promise<ParticipantDemographics>(resolve => resolve({genderList: [], ethnicityList: [], raceList: [], sexAtBirthList: []}));
  }

  findCriteriaMenuOptions(): Promise<CriteriaMenuOptionsListResponse> {
    return new Promise<CriteriaMenuOptionsListResponse>(resolve => resolve({items: []}));
  }

  findDomainInfos(): Promise<DomainInfoResponse> {
    return new Promise<DomainInfoResponse>(resolve => resolve({items: DomainStubVariables.STUB_DOMAINS}));
  }

  findSurveyModules(): Promise<SurveysResponse> {
    return new Promise<SurveysResponse>(resolve => resolve({items: SurveyStubVariables.STUB_SURVEYS}));
  }

  findDomainCount(): Promise<DomainCount> {
    return new Promise<DomainCount>(resolve => resolve(domainCountStub));
  }

  findSurveyCount(): Promise<SurveyCount> {
    return new Promise<SurveyCount>(resolve => resolve(surveyCountStub));
  }
}
