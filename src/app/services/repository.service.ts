// Data interface for getting details about available CDRs. This communicates
// via Angular's builtin Http module with a (fake) REST API.

import 'rxjs/add/operator/toPromise';

import {Injectable} from '@angular/core';
import {Headers, Http} from '@angular/http';

import {Repository} from 'app/models/repository';

@Injectable()
export class RepositoryService {
  private apiUrl = 'api/repository';  // URL to (fake in-memory) web API
  private headers = new Headers({'Content-Type': 'application/json'});

  constructor(private http: Http) {}

  list(): Promise<Repository[]> {
    return this.http
        .get(this.apiUrl)
        .toPromise()
        .then(response => response.json().data as Repository[])
        .catch(this.handleError);
  }

  get(id: number): Promise<Repository> {
    return this.http
        .get(`${this.apiUrl}/${id}`)
        .toPromise()
        .then(response => response.json().data as Repository)
        .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);  // for demo purposes only
    return Promise.reject(error.message || error);
  }
}
