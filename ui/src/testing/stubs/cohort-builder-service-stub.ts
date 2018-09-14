import {DemoChartInfoListResponse} from 'generated';
import {Observable} from 'rxjs/Observable';


export class CohortBuilderServiceStub {

  constructor() {}

  getChartInfo(): Observable<DemoChartInfoListResponse> {
    return Observable.of(<DemoChartInfoListResponse> {items: []});
  }
}
