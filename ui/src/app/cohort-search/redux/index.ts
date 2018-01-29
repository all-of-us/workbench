import {DevToolsExtension, NgRedux} from '@angular-redux/store';
import {Injectable} from '@angular/core';
import {combineEpics, createEpicMiddleware} from 'redux-observable';

import {environment} from 'environments/environment';

import {CohortSearchEpics} from './epics';
import {rootReducer} from './reducer';
import {
  CohortSearchState,
  initialState,
} from './store';

@Injectable()
export class ConfigureStore {

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private epics: CohortSearchEpics,
    private devTools: DevToolsExtension,
  ) {

    let storeEnhancers = [];
    if (environment.debug && devTools.isEnabled()) {
      storeEnhancers = [...storeEnhancers, devTools.enhancer()];
    }

    const middleware = [
      createEpicMiddleware(
        combineEpics(
          epics.fetchCriteria,
          epics.fetchCount,
          epics.fetchChartData,
        )
      )
    ];

    ngRedux.configureStore(
      rootReducer,
      initialState,
      middleware,
      storeEnhancers
    );
  }
}

export * from './actions';
export * from './reducer';
export * from './epics';
export * from './store';
