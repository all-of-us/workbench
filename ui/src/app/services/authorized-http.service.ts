import {Injectable} from '@angular/core';
import {Http, Headers, RequestOptionsArgs, Request, Response, ConnectionBackend, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

// Subclass of Http which adds bearer token auth to all requests.
// Adapted from https://stackoverflow.com/a/40913003 .
@Injectable()
export class AuthorizedHttp extends Http {
  // To get a test token, `gcloud auth login` and then `gcloud auth print-access-token`.
  private bearerToken = 'TODO get from gapi.';

  _addAuthHeader(headers: Headers) {
    headers.append('Authorization', `Bearer ${this.bearerToken}`);
  }

  _setAuthHeader(request: Request) {
    if (!request.headers) {
      request.headers = new Headers()
    }
    this._addAuthHeader(request.headers);
  }

  request(request: string|Request, options?: RequestOptionsArgs): Observable<Response> {
    if (typeof request === 'string') {
      // Requests coming from Http.get() will have a Request object. To support a string URL in
      // place of a request, add the headers on the RequestOptionsArgs instead (as in the original
      // SO example).
      throw new NotImplementedException('AuthorizedHttp.request(url: string) unimplemented.');
    }
    this._setAuthHeader(request);
    return super.request(request, options);
  }
}
