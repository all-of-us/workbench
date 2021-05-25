import {RequestOptionsArgs} from '@angular/common/http/src/interfaces';
import {Observable} from 'rxjs/Observable';

export class HttpStub {

  public get(url: string, options?: RequestOptionsArgs): Observable<Response> {
    return new Observable<Response>(observer => {
      setTimeout(() => {
        observer.next(new Response());
        observer.complete();
      }, 0);
    });
  }
}


