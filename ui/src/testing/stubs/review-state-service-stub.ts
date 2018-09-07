import {AnnotationType, CohortAnnotationDefinition} from 'generated';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';
import {Cohort, CohortReview} from '../../generated';

export class ReviewStateServiceStub {
  public annotationDefinitions = new ReplaySubject<CohortAnnotationDefinition[]>(1);
  public annotationManagerOpen = new BehaviorSubject<boolean>(false);
  public editAnnotationManagerOpen = new BehaviorSubject<boolean>(false);
  public cohort = new ReplaySubject<Cohort>(1);
  public review = new ReplaySubject<CohortReview>(1);

  public annotationDefinitions$ = Observable.of([
    <CohortAnnotationDefinition> {
      cohortAnnotationDefinitionId: 1,
      cohortId: 2,
      columnName: 'test',
      annotationType: AnnotationType.BOOLEAN
    }
  ]);
  public cohort$ = Observable.of(<Cohort>{
    name: 'test',
    criteria: '[]',
    type: 'type'
  });
  public review$ = Observable.of({
    participantCohortStatuses: []
  });

  constructor() {}
}
