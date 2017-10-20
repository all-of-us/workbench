import {Observable} from 'rxjs/Observable';

export class ErrorHandlingServiceStub {
  serverError = false;
  noServerResponse = false;
  serverBusy = false;
  constructor() {}

  public retryApi(observable: Observable<any>): Observable<any> {
    return observable;
  }
}
