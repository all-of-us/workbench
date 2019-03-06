import {CohortReview, ParticipantDataListResponse, ReviewStatus} from 'generated/fetch';
import {Observable} from 'rxjs/Observable';

export const cohortReviewStub = {
  cohortReviewId: 1,
  cohortId: 1,
  cdrVersionId: 1,
  creationTime: '',
  matchedParticipantCount: 1,
  reviewSize: 1,
  reviewedCount: 1,
  queryResultSize: 1,
  reviewStatus: ReviewStatus.CREATED,
  participantCohortStatuses: [],
  page: 1,
  pageSize: 1,
  sortOrder: '',
  sortColumn: '',
};

export class CohortReviewServiceStub {

  constructor() {}

  getParticipantCohortStatuses(): Observable<CohortReview> {
    return Observable.of(cohortReviewStub);
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
  getParticipantCohortStatus() {
    return Observable.of({});
  }
  getParticipantCohortAnnotations() {
    return Observable.of({items: []});
  }
}
