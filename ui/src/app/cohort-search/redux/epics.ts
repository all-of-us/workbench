import {ActionsObservable, Epic} from 'redux-observable';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List, is} from 'immutable';
import {
  BEGIN_CRITERIA_REQUEST,
  CANCEL_CRITERIA_REQUEST,
  CRITERIA_REQUEST_ERROR,
  BEGIN_COUNT_REQUEST,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,
  RootAction,
  ActionTypes,
} from './actions/types';
import {
  loadCriteriaRequestResults,
  loadCountRequestResults,
} from './actions/creators';
import {CohortSearchState} from './store';

import {CohortBuilderService} from 'generated';

type CSEpic = Epic<RootAction, CohortSearchState>;
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
      // appropriate action type.  Path is of form ['criteria', kind, parentId]
      ({path}: ActionTypes[typeof BEGIN_CRITERIA_REQUEST]) => {
        const kind = <string>path.get(1);
        const parentId = <number>path.get(2);
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(_type, parentId)
          .map(result => loadCriteriaRequestResults(path, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(
              (action: ActionTypes[typeof CANCEL_CRITERIA_REQUEST]) =>
              action.path.equals(path))
            .take(1)
          )
          .catch(error => {
            console.log(`Request error: ${JSON.stringify(error, null, 2)}`);
            return Observable.of({type: CRITERIA_REQUEST_ERROR, error, path});
          });
      }
    )
  )

  fetchCounts: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({kind, path, request}: ActionTypes[typeof BEGIN_COUNT_REQUEST]) =>
      this.service.countSubjects(request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadCountRequestResults(kind, count, path))
        .race(action$
          .ofType(CANCEL_COUNT_REQUEST)
          .filter(
            (action: ActionTypes[typeof CANCEL_COUNT_REQUEST]) =>
            is(action.kind, kind) && is(action.path, path))
          .take(1)
        )
        .catch(error => {
          console.log(`Request error: ${JSON.stringify(error, null, 2)}`);
          return Observable.of({type: COUNT_REQUEST_ERROR, error, path});
        })
    )
  )
}
