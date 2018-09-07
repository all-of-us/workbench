import {ChartInfoListResponse} from 'generated';
import {Observable} from 'rxjs/Observable';


export class CohortBuilderServiceStub {

  constructor() {}

  getChartInfo(): Observable<ChartInfoListResponse> {
    return Observable.of(<ChartInfoListResponse> {items: []});
  }
}
