import {Injectable, NgZone} from '@angular/core';
import {Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {ErrorCode, ErrorResponse} from 'generated';

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

  // convert error response from API JSON to ErrorResponse object, otherwise, report parse error
  public static convertAPIError (e: Response) {
    try {
      const { errorClassName = null,
        errorCode = null,
        message = null,
        statusCode = null } = e.json();
      return { errorClassName, errorCode, message, statusCode };
    }  catch {
      return { statusCode: e.status, errorCode: ErrorCode.PARSEERROR };
    }
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

  // don't retry API calls unless the status code is 503.
  public retryApi (observable: Observable<any>, toRun = 3): Observable<any> {
    let numberRuns = 0;

    return observable.retryWhen(errors => {
      return errors.do(e => {
        numberRuns++;
        if (numberRuns === toRun) {
          this.setServerBusy();
          throw e;
        }

        const errorResponse = ErrorHandlingService.convertAPIError(e);
        switch (errorResponse.statusCode) {
          case 503:
            break;
          case 500:
            this.setServerError();
            throw e;
          case 403:
            if (errorResponse.errorCode === ErrorCode.USERDISABLED) {
              this.setUserDisabledError();
            }
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
