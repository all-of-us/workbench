import {ActionsObservable} from 'redux-observable';
import {AnyAction} from 'redux';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List} from 'immutable';

import * as ActionTypes from './actions/types';
import {
  loadCriteriaRequestResults,
  loadCountRequestResults,
  cleanupRequest,
} from './actions/creators';
import {KeyPath, RequestAction} from './typings';

import {CohortBuilderService} from 'generated';


const cancelListener =
  (action$: ActionsObservable<AnyAction>, path: KeyPath): Observable<RequestAction> =>
  action$
    .ofType(ActionTypes.CANCEL_REQUEST)
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

  fetchCriteria = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(ActionTypes.BEGIN_CRITERIA_REQUEST).mergeMap(
      ({path}) => {
        const kind = path.first();
        const parentId = path.last();
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(_type, parentId)
          .map(result => loadCriteriaRequestResults(path, result.items))
          .race(cancelListener(action$, path))
          .catch(error => {
            console.log('Caught an error: ');
            console.dir(error);
            return Observable.of({type: 'ERROR', error});
          });
      }
    )
  )

  fetchSearchResults = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(ActionTypes.BEGIN_COUNT_REQUEST).mergeMap(
      ({path, request}) =>
      this.service.searchSubjects(request)
        .map(count => loadCountRequestResults(path, count))
        .race(cancelListener(action$, path))
        .catch(error => {
          console.log('Caught an error: ');
          console.dir(error);
          return Observable.of({type: 'ERROR', error});
        })
    )
  )

  finalizeRequests = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(
      ActionTypes.LOAD_COUNT_RESULTS,
      ActionTypes.LOAD_CRITERIA_RESULTS,
    ).map(action => action.cleanup)
  )
}
