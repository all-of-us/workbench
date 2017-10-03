import {ActionsObservable} from 'redux-observable';
import {AnyAction} from 'redux';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions as Actions} from './actions';
import {CohortSearchState} from './store.interfaces';

import {CohortBuilderService} from 'generated';


@Injectable()
export class Epics {
  constructor(private service: CohortBuilderService) {}

  fetchCriteria = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(Actions.FETCH_CRITERIA).mergeMap(
      ({critType, parentId}) =>
      this.service.getCriteriaByTypeAndParentId(critType, parentId)
        .map(result => ({type: Actions.LOAD_CRITERIA, children: result.items, critType, parentId}))
        .catch(error => Observable.of({type: Actions.ERROR, error}))
    )
  )
}
