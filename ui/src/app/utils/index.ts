import {ActivatedRoute, Data, Params, Router} from '@angular/router';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

export function isBlank(toTest: String): boolean {
  if (toTest === null) {
    return true;
  } else {
    toTest = toTest.trim();
    return toTest === '';
  }
}

export function deepCopy(obj: Object): Object {
  return fromJS(obj).toJS();
}

/**
 * Navigate a signed out user to the login page from the given relative Angular
 * path.
 */
export function navigateLogin(router: Router, fromUrl: string): Promise<boolean> {
  const params = {};
  if (fromUrl && fromUrl !== '/') {
    params['from'] = fromUrl;
  }
  return router.navigate(['/login', params]);
}

export function flattenedRouteData(route: ActivatedRoute): Observable<Data> {
  return Observable.of(route.pathFromRoot.reduce((res, curr) => {
    return Object.assign({}, res, curr.snapshot.data);
  }, ({})));
}

export function flattenedRouteQueryParams(route: ActivatedRoute): Observable<Params> {
  return Observable.of(route.pathFromRoot.reduce((res, curr) => {
    return Object.assign({}, res, curr.snapshot.queryParams);
  }, ({})));
}
