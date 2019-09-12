import {Response, ResponseOptions} from '@angular/http';
import {Observable} from 'rxjs/Observable';

export class NotebooksServiceStub {

  public setCookieWithHttpInfo(
    googleProject: string, clusterName: string,
    extraHttpRequestParams?: any): Observable<Response> {
    return new Observable<Response>(observer => {
      setTimeout(() => {
        observer.next(new Response(new ResponseOptions({
          status: 200,
          body: ''
        })));
        observer.complete();
      }, 0);
    });
  }
}

