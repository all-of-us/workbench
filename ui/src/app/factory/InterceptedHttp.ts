/*Intercepts all HTTP request */

import {Injectable} from '@angular/core';
import {ConnectionBackend, Http, Request, RequestOptions, RequestOptionsArgs,
  Response} from '@angular/http';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Observable, Subject} from 'rxjs/Rx';

@Injectable()
export class InterceptedHttp extends Http {
  private statusSubject = new Subject<boolean>();
  public shouldPingStatus = true;


  constructor(private backend: ConnectionBackend, private defaultOptions: RequestOptions,
      private errorHandlingService: ErrorHandlingService) {
    super(backend, defaultOptions);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    const response = this.errorHandlingService.retryApi(
        super.request(url, options));
    response.subscribe(() => {}, (e) => {
      if ((e.status === 500 || e.status === 503) &&
          this.shouldPingStatus) {
        this.statusSubject.next(true);
      }
    });
    return response;
  }

  public getStatusSubjectAsObservable(): Observable<boolean> {
    return this.statusSubject.asObservable();
  }


}
