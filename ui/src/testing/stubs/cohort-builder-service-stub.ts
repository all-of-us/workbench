import {
  CohortBuilderApi, Criteria,
  CriteriaAttributeListResponse,
  CriteriaListResponse,
  DemoChartInfoListResponse,
  ParticipantDemographics,
  TreeSubType,
  TreeType
} from 'generated/fetch';

export const cohortStub = {
  name: 'Test Cohort',
  criteria: '{}',
  type: '',
};

const criteriaStub = {
  id: 1,
  parentId: 0,
  type: TreeType.ICD9,
  subtype: TreeSubType.CM,
  code: '123',
  name: 'Test',
  count: 1,
  group: false,
  selectable: true,
  conceptId: 123,
  domainId: '',
  hasAttributes: false,
  path: '0',
};

export class CohortBuilderServiceStub extends CohortBuilderApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getDemoChartInfo(): Promise<DemoChartInfoListResponse> {
    return new Promise<DemoChartInfoListResponse>(resolve => resolve({items: []}));
  }

  countParticipants(): Promise<number> {
    return new Promise<number>(resolve => resolve(1));
  }

  getCriteriaAttributeByConceptId(): Promise<CriteriaAttributeListResponse> {
    return new Promise<CriteriaAttributeListResponse>(resolve => resolve({items: []}));
  }

  getCriteriaAutoComplete(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  getCriteriaBy(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  getDrugBrandOrIngredientByValue(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  getDrugIngredientByConceptId(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve({items: []}));
  }

  getPPICriteriaParent(): Promise<Criteria> {
    return new Promise<Criteria>(resolve => resolve(criteriaStub));
  }

  getParticipantDemographics(): Promise<ParticipantDemographics> {
    return new Promise<ParticipantDemographics>(resolve =>
      resolve({genderList: [], ethnicityList: [], raceList: []}));
  }
}
