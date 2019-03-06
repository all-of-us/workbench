import {
  CohortChartDataListResponse,
  CohortReview,
  CohortReviewApi,
  CohortStatus,
  EmptyResponse,
  ParticipantChartDataListResponse,
  ParticipantCohortAnnotation,
  ParticipantCohortAnnotationListResponse,
  ParticipantCohortStatus,
  ParticipantDataListResponse,
  ReviewStatus,
  SortOrder
} from 'generated/fetch';

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

const participantAnnotationStub = {
  cohortAnnotationDefinitionId: 1,
  cohortReviewId: 1,
  participantId: 1,
};

const participantDataListResponseStub = {
  items: [],
  count: 1,
  pageRequest: {
    page: 1,
    pageSize: 25,
    sortOrder: SortOrder.Asc,
    sortColumn: 'test'
  }
};

export class CohortReviewServiceStub extends CohortReviewApi {
  configuration;
  basePath;
  fetch;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getParticipantCohortStatuses(): Promise<CohortReview> {
    return new Promise<CohortReview>(resolve => resolve(cohortReviewStub));
  }

  getParticipantData(): Promise<ParticipantDataListResponse> {
    return new Promise<ParticipantDataListResponse>(resolve =>
      resolve(participantDataListResponseStub));
  }
  getParticipantChartData(): Promise<ParticipantChartDataListResponse> {
    return new Promise<ParticipantChartDataListResponse>(resolve => resolve({items: []}));
  }
  getCohortChartData(): Promise<CohortChartDataListResponse> {
    return new Promise<CohortChartDataListResponse>(resolve => resolve({count: 1, items: []}));
  }
  getParticipantCohortStatus(): Promise<ParticipantCohortStatus> {
    return new Promise<ParticipantCohortStatus>(resolve =>
      resolve({participantId: 1, status: CohortStatus.NOTREVIEWED}));
  }
  getParticipantCohortAnnotations(): Promise<ParticipantCohortAnnotationListResponse> {
    return new Promise<ParticipantCohortAnnotationListResponse>(resolve => resolve({items: []}));
  }
  createParticipantCohortAnnotation() {
    return new Promise<ParticipantCohortAnnotation>(resolve => resolve(participantAnnotationStub));
  }
  deleteParticipantCohortAnnotation() {
    return new Promise<EmptyResponse>(resolve => resolve({}));
  }
  updateParticipantCohortAnnotation(): Promise<ParticipantCohortAnnotation> {
    return new Promise<ParticipantCohortAnnotation>(resolve =>
      resolve({cohortAnnotationDefinitionId: 1, cohortReviewId: 1, participantId: 1}));
  }
  updateParticipantCohortStatus(): Promise<ParticipantCohortStatus> {
    return new Promise<ParticipantCohortStatus>(resolve =>
      resolve({participantId: 1, status: CohortStatus.NOTREVIEWED}));
  }
  createCohortReview() {
    // return Observable.of({});
    return new Promise<CohortReview>(resolve => {
      resolve(cohortReviewStub);
    });
  }
}
