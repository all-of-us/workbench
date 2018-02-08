/*Intercepts all HTTP request */

import {Injectable, NgZone} from '@angular/core';
import {ConnectionBackend, Http, Response, Request, RequestOptions,
  RequestOptionsArgs} from '@angular/http';
import {environment} from '../../environments/environment';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {overriddenUrlKey} from 'app/views/app/component';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {ConfigService} from 'generated';
import {Observable} from 'rxjs/Rx';

@Injectable()
export class InterceptedHttp extends Http {

  private signInService: SignInService;

  constructor(private backend: ConnectionBackend, private defaultOptions: RequestOptions,
      private errorHandlingService: ErrorHandlingService, private ngZone: NgZone) {
    super(backend, defaultOptions);
    const configService = new ConfigService(this, localStorage.getItem(overriddenUrlKey)
        || environment.allOfUsApiUrl, null);
    const serverConfigService = new ServerConfigService(configService, errorHandlingService);
    this.signInService = new SignInService(ngZone, serverConfigService);
  }

  request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
    if (this.signInService && this.signInService.auth2) {
      this.signInService.setUserLastActivityTime();
    }
    return super.request(url, options);
  }
}

