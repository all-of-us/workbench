import {DevToolsExtension, NgRedux} from '@angular-redux/store';
import {Injectable} from '@angular/core';
import {combineEpics, createEpicMiddleware} from 'redux-observable';


import {CohortSearchEpics} from './epics';
import {rootReducer} from './reducer';
import {
  CohortSearchState,
  initialState,
} from './store';

@Injectable()
export class ConfigureStore {

  constructor(
    ngRedux: NgRedux<CohortSearchState>,
    epics: CohortSearchEpics,
    devTools: DevToolsExtension,
  ) {

    let storeEnhancers = [];
    if (devTools.isEnabled()) {
      storeEnhancers = [...storeEnhancers, devTools.enhancer()];
    }

    const middleware = [
      createEpicMiddleware(
        combineEpics(
          epics.fetchCriteria,
          epics.fetchCriteriaBySubtype,
          epics.fetchAllCriteria,
          epics.fetchDrugCriteria,
          epics.fetchAutocompleteOptions,
          epics.fetchIngredientsForBrand,
          epics.fetchAttributes,
          epics.fetchCount,
          epics.fetchChartData,
          epics.previewCount,
          epics.attributePreviewCount,
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
