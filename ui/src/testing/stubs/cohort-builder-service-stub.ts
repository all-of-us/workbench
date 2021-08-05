import {
  CohortBuilderApi,
  Criteria,
  CriteriaAttributeListResponse,
  CriteriaListResponse,
  CriteriaMenuListResponse,
  CriteriaSubType,
  CriteriaType,
  DemoChartInfoListResponse,
  Domain,
  DomainCount,
  DomainInfo,
  DomainInfoResponse,
  ParticipantDemographics,
  SurveyCount,
  SurveyModule,
  SurveysResponse,
  SurveyVersionListResponse
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils'

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

export const CriteriaStubVariables: Criteria[] = [
  {
    id: 1,
    parentId: 0,
    type: CriteriaType.ICD9CM.toString(),
    subtype: '',
    code: '123',
    name: 'Test',
    count: 1,
    group: false,
    selectable: true,
    conceptId: 123,
    domainId: Domain.CONDITION.toString(),
    hasAttributes: false,
    path: '0',
  },
  {
    childCount: 44652,
    code: '',
    conceptId: 38003563,
    count: 44652,
    domainId: 'PERSON',
    group: false,
    hasAncestorData: false,
    hasAttributes: false,
    hasHierarchy: false,
    id: 333112,
    isStandard: true,
    name: 'Ethnicity - Hispanic or Latino',
    parentCount: 0,
    parentId: 0,
    path: '',
    selectable: true,
    subtype: '',
    type: 'ETHNICITY',
    value: '',
  }
];

export const CriteriaWithAttributesStubVariables: Criteria[] = [
  {
    childCount: 197614,
    code: '',
    conceptId: 903133,
    count: 197614,
    domainId: Domain.PHYSICALMEASUREMENT.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 328005,
    isStandard: false,
    name: 'Height',
    parentCount: 0,
    parentId: 0,
    path: '',
    selectable: true,
    subtype: CriteriaSubType.HEIGHT.toString(),
    type: CriteriaType.PPI.toString(),
    value: '',
  },
  {
    childCount: 196156,
    code: '',
    conceptId: null,
    count: 196156,
    domainId: Domain.PHYSICALMEASUREMENT.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 327999,
    isStandard: false,
    name: 'Blood Pressure',
    parentCount: 0,
    parentId: 327994,
    path: '',
    selectable: true,
    subtype: CriteriaSubType.BP.toString(),
    type: CriteriaType.PPI.toString(),
    value: '',
  },
  {
    childCount: 197912,
    code: '29463-7',
    conceptId: 3025315,
    count: 197912,
    domainId: Domain.MEASUREMENT.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 3623241,
    isStandard: true,
    name: 'Body weight',
    parentCount: 0,
    parentId: 3623181,
    path: '3623174.3623181.3623241',
    selectable: true,
    subtype: CriteriaSubType.CLIN.toString(),
    type: CriteriaType.LOINC.toString(),
    value: '',
  },
  {
    childCount: -1,
    code: 'LivingSituation_HowManyPeople',
    conceptId: 1585889,
    count: -1,
    domainId: Domain.SURVEY.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 328048,
    isStandard: false,
    name: 'Select a value',
    parentCount: -1,
    parentId: 328047,
    path: '328012.328047.328048',
    selectable: true,
    subtype: CriteriaSubType.ANSWER.toString(),
    type: CriteriaType.PPI.toString(),
    value: ''
  },
  {
    childCount: 76023,
    code: 'cdc_covid_19_21',
    conceptId: 1333102,
    count: 76023,
    domainId: Domain.SURVEY.toString(),
    group: true,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 331392,
    isStandard: false,
    name: 'In the past month, have recommendations for socially distancing caused stress for you?',
    parentCount: 76023,
    parentId: 331391,
    path: '331390.331391.331392',
    selectable: true,
    subtype: CriteriaSubType.QUESTION.toString(),
    type: CriteriaType.PPI.toString(),
    value: ''
  },
  {
    childCount: 8064,
    code: 'cdc_covid_19_21',
    conceptId: 1333102,
    count: 8064,
    domainId: Domain.SURVEY.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 331393,
    isStandard: false,
    name: 'A lot',
    parentCount: 0,
    parentId: 331392,
    path: '331390.331391.331392.331393',
    selectable: true,
    subtype: CriteriaSubType.ANSWER.toString(),
    type: CriteriaType.PPI.toString(),
    value: '1332897',
  },
  {
    childCount: -1,
    code: 'basics_xx',
    conceptId: 1333015,
    count: -1,
    domainId: Domain.SURVEY.toString(),
    group: false,
    hasAncestorData: false,
    hasAttributes: true,
    hasHierarchy: true,
    id: 331598,
    isStandard: false,
    name: 'Select a value',
    parentCount: -1,
    parentId: 331597,
    path: '331390.331596.331597.331598',
    selectable: true,
    subtype: CriteriaSubType.ANSWER.toString(),
    type: CriteriaType.PPI.toString(),
    value: '',
  }
];

export const RootSurveyStubVariables: Criteria[] = [
  {
    childCount: 0,
    code: 'TheBasics',
    conceptId: 1586134,
    count: 234525,
    domainId: Domain.SURVEY.toString(),
    group: true,
    hasAncestorData: false,
    hasAttributes: false,
    hasHierarchy: true,
    id: 328012,
    isStandard: false,
    name: 'The Basics',
    parentCount: 234525,
    parentId: 0,
    path: '328012',
    selectable: true,
    subtype: CriteriaSubType.SURVEY.toString(),
    type: CriteriaType.PPI.toString(),
    value: ''
  },
  {
    childCount: 0,
    code: 'cope',
    conceptId: 1333342,
    count: 76826,
    domainId: Domain.SURVEY.toString(),
    group: true,
    hasAncestorData: false,
    hasAttributes: false,
    hasHierarchy: true,
    id: 331390,
    isStandard: false,
    name: 'COVID-19 Participant Experience (COPE) Survey',
    parentCount: 76826,
    parentId: 0,
    path: '331390',
    selectable: true,
    subtype: CriteriaSubType.SURVEY.toString(),
    type: CriteriaType.PPI.toString(),
    value: '',
  }
];

export const SurveyQuestionStubVariables = {
  328047: {
    conceptId: 1585889,
    count: 234517,
    name: 'Not including yourself, how many other people live at home with you?',
  },
  331392: {
    conceptId: 1333102,
    count: 76023,
    name: 'In the past month, have recommendations for socially distancing caused stress for you?',
  },
  331597: {
    conceptId: 1333015,
    count: 76003,
    name: 'Not including yourself, how many other people live at home with you?',
  },
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
    return new Promise<Criteria>(resolve => resolve(CriteriaStubVariables[0]));
  }

  findParticipantDemographics(): Promise<ParticipantDemographics> {
    return new Promise<ParticipantDemographics>(resolve => resolve({genderList: [], ethnicityList: [], raceList: [], sexAtBirthList: []}));
  }

  findCriteriaMenuOld(): Promise<CriteriaMenuListResponse> {
    return new Promise<CriteriaMenuListResponse>(resolve => resolve({items: []}));
  }

  findDomainInfosDepreciate(): Promise<DomainInfoResponse> {
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

  findSurveyVersionByQuestionConceptId(): Promise<SurveyVersionListResponse> {
    return new Promise<SurveyVersionListResponse>(resolve => resolve({items: []}));
  }

  findSurveyVersionByQuestionConceptIdAndAnswerConceptId(): Promise<SurveyVersionListResponse> {
    return new Promise<SurveyVersionListResponse>(resolve => resolve({items: []}));
  }
}
