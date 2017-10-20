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
  criteriaRequestError,
  countRequestError,
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
      ({kind, parentId}: ActionTypes[typeof BEGIN_CRITERIA_REQUEST]) => {
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(_type, parentId)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(
              (action: ActionTypes[typeof CANCEL_CRITERIA_REQUEST]) =>
              action.kind === kind && action.parentId === parentId
            )
            .take(1)
          )
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchCounts: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({entityType, entityId, request}: ActionTypes[typeof BEGIN_COUNT_REQUEST]) =>
      this.service.countSubjects(request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadCountRequestResults(entityType, entityId, count))
        .race(action$
          .ofType(CANCEL_COUNT_REQUEST)
          .filter(
            (action: ActionTypes[typeof CANCEL_COUNT_REQUEST]) =>
            action.entityType === entityType && action.entityId === entityId
          )
          .take(1)
        )
        .catch(e => Observable.of(countRequestError(entityType, entityId, e)))
    )
  )
}
