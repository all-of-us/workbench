import {Router} from '@angular/router';
import {fromJS} from 'immutable';

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
