import {
  CohortChartDataListResponse,
  CohortReview,
  CohortReviewApi,
  CohortStatus,
  DomainType,
  EmptyResponse,
  ParticipantChartDataListResponse,
  ParticipantCohortAnnotation,
  ParticipantCohortAnnotationListResponse,
  ParticipantCohortStatus,
  ParticipantDataListResponse,
  ReviewStatus,
  SortOrder,
  VocabularyListResponse
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
  participantCohortStatuses: [{participantId: 1, status: CohortStatus.NOTREVIEWED}],
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

const participantDataStub = {
  itemDate: '',
  standardName: '',
  ageAtEvent: 22,
  domainType: DomainType.CONDITION
};

const participantDataListResponseStub = {
  items: [participantDataStub],
  count: 1,
  pageRequest: {
    page: 1,
    pageSize: 25,
    sortOrder: SortOrder.Asc,
    sortColumn: 'test'
  }
};

const participantChartDataStub = {
  standardName: '',
  standardVocabulary: '',
  startDate: '',
  ageAtEvent: 22,
  rank: 1
};

const cohortChartDataStub = {
  name: 'Test',
  conceptId: 123,
  count: 1
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
    return new Promise<ParticipantChartDataListResponse>(resolve =>
      resolve({items: [participantChartDataStub]}));
  }
  getCohortChartData(): Promise<CohortChartDataListResponse> {
    return new Promise<CohortChartDataListResponse>(resolve =>
      resolve({count: 1, items: [cohortChartDataStub]}));
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
    return new Promise<CohortReview>(resolve => {
      resolve(cohortReviewStub);
    });
  }
  getVocabularies(): Promise<VocabularyListResponse> {
    return new Promise<VocabularyListResponse>(resolve => resolve({items: []}));
  }
}
