import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionApi, CohortAnnotationDefinitionListResponse,
  EmptyResponse
} from 'generated/fetch';

export const cohortAnnotationDefinitionStub = {
  cohortAnnotationDefinitionId: 1,
  cohortId: 2,
  columnName: 'test',
  annotationType: AnnotationType.BOOLEAN
};

export class CohortAnnotationDefinitionServiceStub extends CohortAnnotationDefinitionApi {
  createCohortAnnotationDefinition(): Promise<CohortAnnotationDefinition> {
    return new Promise<CohortAnnotationDefinition>(resolve =>
      resolve(cohortAnnotationDefinitionStub));
  }

  deleteCohortAnnotationDefinition(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => resolve());
  }

  getCohortAnnotationDefinition(): Promise<CohortAnnotationDefinition> {
    return new Promise<CohortAnnotationDefinition>(resolve =>
      resolve(cohortAnnotationDefinitionStub));
  }

  getCohortAnnotationDefinitions(): Promise<CohortAnnotationDefinitionListResponse> {
    return new Promise<CohortAnnotationDefinitionListResponse>(resolve => resolve({items: []}));
  }

  updateCohortAnnotationDefinition(): Promise<CohortAnnotationDefinition> {
    return new Promise<CohortAnnotationDefinition>(resolve =>
      resolve(cohortAnnotationDefinitionStub));
  }
}
