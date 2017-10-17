import {Observable} from 'rxjs/Observable';

export class ErrorHandlingServiceStub {
  fiveHundred = false;
  zero = false;
  constructor() {}

  public retryApi(observable: Observable<any>): Observable<any> {
    return observable;
  }
}
