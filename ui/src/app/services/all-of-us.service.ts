// Mediate communication with the All of Us backend server, for workspace
// management etc.

import 'rxjs/add/operator/toPromise';

import {Injectable} from '@angular/core';

import {environment} from 'environments/environment';
import {AuthorizedHttp} from 'app/services/authorized-http.service';

@Injectable()
export class AllOfUsService {
  constructor(private http: AuthorizedHttp) {}

  // Gets a "hello world" value from the server (a test endpoint).
  getHelloWorld(): Promise<String> {
    return this.http
        .get(environment.allOfUsApiUrl)
        .toPromise()
        .then(response => response.text());
  }
}
