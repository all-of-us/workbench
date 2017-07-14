// Mediate communication with the All of Us backend server, for workspace
// management etc.

import 'rxjs/add/operator/toPromise';

import {Http} from '@angular/http';
import {Injectable} from '@angular/core';

import {environment} from 'environments/environment';

@Injectable()
export class AllOfUsService {
  constructor(private http: Http) {}

  // Gets a "hello world" value from the server (a test endpoint).
  getHelloWorld(): Promise<String> {
    return this.http
        .get(environment.allOfUsApiUrl)
        .toPromise()
        .then(response => response.text());
  }
}
