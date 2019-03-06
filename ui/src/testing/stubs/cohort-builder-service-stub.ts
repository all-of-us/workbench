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

  getParticipantDemographics() {
    return Observable.of({raceList: [], genderList: [], ethnicityList: []});
  }

  countParticipants() {}

  getCriteriaAttributeByConceptId() {}

  getCriteriaAutoComplete() {}

  getCriteriaBy() {}

  getDrugBrandOrIngredientByValue() {}

  getDrugIngredientByConceptId() {}

  getPPICriteriaParent() {}
}
