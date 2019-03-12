import {AnnotationType} from 'generated';
import {Observable} from 'rxjs/Observable';

export const cohortAnnotationDefinitionStub = {
  cohortAnnotationDefinitionId: 1,
  cohortId: 2,
  columnName: 'test',
  annotationType: AnnotationType.BOOLEAN
};

export class CohortAnnotationDefinitionServiceStub {
  getCohortAnnotationDefinitions() {
    return Observable.of({items: []});
  }
}
