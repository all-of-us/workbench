/*
 * Handles the status of an outbound request: pending, cancelled, etc
 */
import {AnyAction} from 'redux';
import {ActionsObservable} from 'redux-observable';
import {List, Map, Set} from 'immutable';
import {Observable} from 'rxjs/Observable';


const STATE_KEY = 'requests';
const START_REQUEST   = 'START_REQUEST';
const CANCEL_REQUEST  = 'CANCEL_REQUEST';
const CLEANUP_REQUEST = 'CLEANUP_REQUEST';

type KeyPath = List<string | number>;

interface RequestAction extends AnyAction {
  type: 'START_REQUEST' | 'CANCEL_REQUEST' | 'CLEANUP_REQUEST';
  path: KeyPath;
}


/* Action creators
 */
export const startRequest =
  (path: KeyPath): RequestAction => ({type: START_REQUEST, path});

export const cancelRequest =
  (path: KeyPath): RequestAction => ({type: CANCEL_REQUEST, path});

export const cleanupRequest =
  (path: KeyPath): RequestAction => ({type: CLEANUP_REQUEST, path});


/* Utility functions (selectors, stream watchers, etc)
 */
export const isRequesting = (objPath: KeyPath) =>
  (state): boolean => state.get(STATE_KEY).has(objPath);

export const cancelListener =
  (action$: ActionsObservable<AnyAction>, path: KeyPath): Observable<RequestAction> =>
  action$
    .ofType(CANCEL_REQUEST)
    .filter(action => action.path.equals(path))
    .map(action => cleanupRequest(path))
    .take(1);


/* Reducer
 */
type _StateKey = 'requests';
export type RequestState = Map<_StateKey, Set<KeyPath>>;
type _State = RequestState & Map<any, any>;

export default function(state: _State, action: RequestAction): _State {
  switch (action.type) {
    case START_REQUEST:
      return state.update(STATE_KEY, activeRequests => activeRequests.add(action.path));

    case CLEANUP_REQUEST:
      return state.update(STATE_KEY, activeRequests => activeRequests.delete(action.path));

    default:
      return state;
  }
}
