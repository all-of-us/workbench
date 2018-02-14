/*Intercepts all HTTP request */

import {Injectable} from '@angular/core';
import {ConnectionBackend, Http, Request, RequestOptions, RequestOptionsArgs,
  Response} from '@angular/http';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Observable} from 'rxjs/Rx';

@Injectable()
export class InterceptedHttp extends Http {

  constructor(private backend: ConnectionBackend, private defaultOptions: RequestOptions,
      private errorHandlingService: ErrorHandlingService) {
    super(backend, defaultOptions);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    return this.errorHandlingService.retryApi(
        super.request(url, options));
  }
}

