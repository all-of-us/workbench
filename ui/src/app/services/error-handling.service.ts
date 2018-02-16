import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/Observable';


@Injectable()
export class ErrorHandlingService {

  public apiDown: boolean;
  public firecloudDown: boolean;
  public notebooksDown: boolean;
  public serverError: boolean;
  public noServerResponse: boolean;
  public serverBusy: boolean;

  public userDisabledError: boolean;

  constructor(private zone: NgZone) {
    this.serverError = false;
    this.noServerResponse = false;
  }

  public setServerError(): void {
    this.serverError = true;
  }

  public clearServerError(): void {
    this.serverError = false;
  }

  public setUserDisabledError(): void {
    this.userDisabledError = true;
  }

  public clearUserDisabledError(): void {
    this.userDisabledError = false;
  }

  public setNoServerResponse(): void {
    this.noServerResponse = true;
  }

  public clearNoServerResponse(): void {
    this.noServerResponse = false;
  }

  public setServerBusy(): void {
    this.serverBusy = true;
  }

  public clearServerBusy(): void {
    this.serverBusy = false;
  }

  // Don't retry API calls unless the status code is 503.
  public retryApi (observable: Observable<any>,
      toRun?: number): Observable<any> {
    if (toRun === undefined) {
      toRun = 3;
    }
    let numberRuns = 0;

    return observable.retryWhen((errors) => {
      return errors.do((e) => {
        numberRuns++;
        if (numberRuns === toRun) {
          this.setServerBusy();
          throw e;
        }
        switch (e.status) {
          case 503:
            break;
          case 500:
            this.setServerError();
            throw e;
          case 403:
            this.setUserDisabledError();
            throw e;
          case 0:
            this.setNoServerResponse();
            throw e;
          default:
            throw e;
        }
      });
    });
  }
}
