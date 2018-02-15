/*Intercepts all HTTP request */

import {Injectable} from '@angular/core';
import {ConnectionBackend, Http, Request, RequestOptions, RequestOptionsArgs,
  Response} from '@angular/http';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Observable} from 'rxjs/Rx';

import {getStatusService} from 'app/app.module';

import {StatusService} from 'generated';

@Injectable()
export class InterceptedHttp extends Http {
  private statusService: StatusService = null;
  constructor(private backend: ConnectionBackend, private defaultOptions: RequestOptions,
      private errorHandlingService: ErrorHandlingService) {
    super(backend, defaultOptions);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    const response = this.errorHandlingService.retryApi(
        super.request(url, options));
    response.subscribe(() => {}, (e) => {
      if ((e.status === 500 || e.status === 503) &&
          !this.errorHandlingService.apiDown &&
          !this.errorHandlingService.notebooksDown &&
          !this.errorHandlingService.firecloudDown) {
        this.getApiStatus();
      }
    });
    return response;
  }

  private getApiStatus(): void {
    if (this.statusService === null) {
      this.statusService = getStatusService(this);
    }
    this.statusService.getStatus().subscribe((resp) => {
      if (resp.firecloudStatus === false) {
        this.errorHandlingService.firecloudDown = true;
      }
      if (resp.notebooksStatus === false) {
        this.errorHandlingService.notebooksDown = true;
      }
      return;
    }, () => {
      this.errorHandlingService.apiDown = true;
      return;
    });
  }
}
