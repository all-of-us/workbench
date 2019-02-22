import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export class ReviewStateServiceStub {
  public annotationManagerOpen = new BehaviorSubject<boolean>(false);
  public editAnnotationManagerOpen = new BehaviorSubject<boolean>(false);

  constructor() {}
}
