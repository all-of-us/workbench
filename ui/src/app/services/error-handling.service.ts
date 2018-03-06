import {Injectable, NgZone} from '@angular/core';
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
  public errorResponse: ErrorResponse;

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

        this.errorResponse = this.convertAPIError(e);
        switch (this.errorResponse.statusCode) {
          case 503:
            break;
          case 500:
            this.setServerError();
            throw e;
          case 403:
            if (this.errorResponse.errorCode === ErrorCode.USERDISABLED) {
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

  // convert error response from API to ErrorResponse object
  public convertAPIError (e: any) {
    if (e._body != null && JSON.parse(e._body) != null) {
      const convertedError: ErrorResponse = {
        'errorClassName': JSON.parse(e._body).errorClassName || '',
        'errorCode': JSON.parse(e._body).errorCode || '',
        'message': JSON.parse(e._body).message || '',
        'statusCode': JSON.parse(e._body).statusCode || ''
      };
      return convertedError;
    }
    return {};
  }
}
