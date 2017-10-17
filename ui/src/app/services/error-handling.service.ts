import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class ErrorHandlingService {

  // Expose "current user details" as an Observable
  public fiveHundred: boolean;
  public zero: boolean;

  constructor(private zone: NgZone) {
    this.fiveHundred = false;
    this.zero = false;
  }

  public publishFiveHundred(): void {
    this.fiveHundred = true;
  }

  public resolveFiveHundred(): void {
    this.fiveHundred = false;
  }

  public publishZero(): void {
    this.zero = true;
  }

  public resolveZero(): void {
    this.zero = false;
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
        switch (e.status) {
          case 503:
            break;
          case 500:
            this.publishFiveHundred();
            throw e;
          case 0:
            this.publishZero();
            throw e;
          default:
            throw e;

        }
      });
    });
  }

}
