import {CohortReview, ParticipantDataListResponse} from 'generated';
import {Observable} from 'rxjs/Observable';

export class CohortReviewServiceStub {

  constructor() {}

  getParticipantCohortStatuses(): Observable<CohortReview> {
    return Observable.of(<CohortReview> {});
  }

  getParticipantData(): Observable<ParticipantDataListResponse> {
    return Observable.of(<ParticipantDataListResponse> {items: []});
  }
  getParticipantChartData(): Observable<ParticipantDataListResponse> {
    return Observable.of(<ParticipantDataListResponse> {items: []});
  }
  getCohortChartData(): Observable<ParticipantDataListResponse> {
    return Observable.of(<ParticipantDataListResponse> {items: []});
  }
}
