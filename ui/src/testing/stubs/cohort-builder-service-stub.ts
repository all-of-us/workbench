import {
  CohortBuilderApi, Criteria,
  CriteriaAttributeListResponse,
  CriteriaListResponse,
  DemoChartInfoListResponse,
  ParticipantDemographics
} from 'generated/fetch';

export const cohortStub = {
  name: 'Test Cohort',
  criteria: '{}',
  type: '',
};

export class CohortBuilderServiceStub extends CohortBuilderApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getDemoChartInfo(): Promise<DemoChartInfoListResponse> {
    return new Promise<DemoChartInfoListResponse>(resolve => resolve());
  }

  countParticipants(): Promise<number> {
    return new Promise<number>(resolve => resolve(1));
  }

  getCriteriaAttributeByConceptId(): Promise<CriteriaAttributeListResponse> {
    return new Promise<CriteriaAttributeListResponse>(resolve => resolve());
  }

  getCriteriaAutoComplete(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve());
  }

  getCriteriaBy(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve());
  }

  getDrugBrandOrIngredientByValue(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve());
  }

  getDrugIngredientByConceptId(): Promise<CriteriaListResponse> {
    return new Promise<CriteriaListResponse>(resolve => resolve());
  }

  getPPICriteriaParent(): Promise<Criteria> {
    return new Promise<Criteria>(resolve => resolve());
  }

  getParticipantDemographics(): Promise<ParticipantDemographics> {
    return new Promise<ParticipantDemographics>(resolve => resolve());
  }
}
