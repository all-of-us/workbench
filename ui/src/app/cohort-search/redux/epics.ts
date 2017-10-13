import {ActionsObservable, Epic} from 'redux-observable';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List} from 'immutable';

import {
  CANCEL_REQUEST,
  BEGIN_CRITERIA_REQUEST,
  BEGIN_COUNT_REQUEST,
  LOAD_COUNT_RESULTS,
  LOAD_CRITERIA_RESULTS,
  REQUEST_ERROR,
  RootAction,
  ActionTypes,
} from './actions/types';
import {
  loadCriteriaRequestResults,
  loadCountRequestResults,
  cleanupRequest,
  requestError,
} from './actions/creators';
import {CohortSearchState} from './store';

import {CohortBuilderService} from 'generated';

type CSEpic = Epic<RootAction, CohortSearchState>;
type NeedsCleanup =
    ActionTypes[typeof LOAD_CRITERIA_RESULTS]
  | ActionTypes[typeof LOAD_COUNT_RESULTS]
  | ActionTypes[typeof REQUEST_ERROR]
  ;

const cancelListener =
  (action$, path) =>
  action$
    .ofType(CANCEL_REQUEST)
    .filter(action => action.path.equals(path))
    .map(action => cleanupRequest(path))
    .take(1);

/**
 * CohortSearchEpics
 *
 * Exposes functions (called `epics` by redux-observable) that listen in on the
 * stream of dispatched actions (exposed as an Observable) and attach handlers
 * to certain of them; this allows us to dispatch actions asynchronously.  This is
 * the interface between the application state and the backend API.
 */
@Injectable()
export class CohortSearchEpics {
  constructor(private service: CohortBuilderService) {}

  fetchCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_CRITERIA_REQUEST).mergeMap(
      // For the TypeScript compiler to allow the destructuring, we cast to the
      // appropriate action type
      ({path}: ActionTypes[typeof BEGIN_CRITERIA_REQUEST]) => {
        const kind = <string>path.first();
        const parentId = <number>path.last();
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(_type, parentId)
          .map(result => loadCriteriaRequestResults(path, result.items))
          .race(cancelListener(action$, path))
          .catch(error => {
            console.log('Caught an error: ');
            console.dir(error);
            return Observable.of(requestError(error, path));
          });
      }
    )
  )

  fetchCounts: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({path, request}: ActionTypes[typeof BEGIN_COUNT_REQUEST]) =>
      this.service.countSubjects(request)
        .map(count => loadCountRequestResults(path, count))
        .race(cancelListener(action$, path))
        .catch(error => {
          console.log('Caught an error: ');
          console.dir(error);
            return Observable.of(requestError(error, path));
        })
    )
  )

  finalizeRequests: CSEpic = (action$) => (
    action$.ofType(
      LOAD_COUNT_RESULTS,
      LOAD_CRITERIA_RESULTS,
      REQUEST_ERROR,
    ).map((action: NeedsCleanup) => action.cleanup)
  )
}
