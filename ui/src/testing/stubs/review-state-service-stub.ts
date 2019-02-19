import {Cohort} from 'generated';
import {AnnotationType, CohortAnnotationDefinition} from 'generated';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';

export class ReviewStateServiceStub {
  public annotationManagerOpen = new BehaviorSubject<boolean>(false);
  public editAnnotationManagerOpen = new BehaviorSubject<boolean>(false);

  constructor() {}
}
