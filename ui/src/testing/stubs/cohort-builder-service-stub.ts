import {DemoChartInfoListResponse} from 'generated';
import {Observable} from 'rxjs/Observable';

export const cohortStub = {
  name: 'Test Cohort',
  criteria: '{}',
  type: '',
};

export class CohortBuilderServiceStub {

  constructor() {}

  getDemoChartInfo(): Observable<DemoChartInfoListResponse> {
    return Observable.of(<DemoChartInfoListResponse> {items: []});
  }

  countParticipants() {}

  getCriteriaAttributeByConceptId() {}

  getCriteriaAutoComplete() {}

  getCriteriaBy() {}

  getDrugBrandOrIngredientByValue() {}

  getDrugIngredientByConceptId() {}

  getPPICriteriaParent() {}

  getParticipantDemographics() {}
}
