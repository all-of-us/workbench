// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule, NgRedux, DevToolsExtension} from '@angular-redux/store';
import {createEpicMiddleware, combineEpics} from 'redux-observable';

/* Components */
import {AddCriteriaComponent} from './add-criteria.component';
import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {WizardCriteriaGroupComponent} from './wizard-criteria-group/wizard-criteria-group.component';
import {WizardModalComponent} from './wizard-modal/wizard-modal.component';

import {
  CriteriaTreeNodeComponent,
  CriteriaTreeRootComponent,
  NodeInfoComponent,
} from './criteria-tree';

import {
  ChartsComponent,
  GenderChartComponent,
  RaceChartComponent,
  GoogleChartComponent,
} from './charts';

/* Other Objects */
import {CohortSearchActions} from './actions';
import {CohortSearchState, InitialState} from './store';
import {CohortSearchEpics} from './epics';
import {CohortSearchRouter} from './router';
import {environment} from 'environments/environment';
import {rootReducer} from './reducer';

import {CohortBuilderService} from 'generated';

// tslint:enable:max-line-length

@NgModule({
  imports: [
    ClarityModule,
    CohortSearchRouter,
    CommonModule,
    NgReduxModule,
  ],
  declarations: [
    AddCriteriaComponent,
    CohortBuilderComponent,

    CriteriaTreeRootComponent,
    CriteriaTreeNodeComponent,
    NodeInfoComponent,

    SearchGroupComponent,
    SearchGroupItemComponent,
    WizardModalComponent,
    WizardCriteriaGroupComponent,

    ChartsComponent,
    GenderChartComponent,
    RaceChartComponent,
    GoogleChartComponent,
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    CohortBuilderService,
    CohortSearchActions,
    CohortSearchEpics,
  ]
})
export class CohortSearchModule {
  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private epics: CohortSearchEpics,
              private devTools: DevToolsExtension) {

    let storeEnhancers = [];
    if (environment.debug && devTools.isEnabled()) {
      storeEnhancers = [...storeEnhancers, devTools.enhancer()];
    }

    const middleware = [
      createEpicMiddleware(
        combineEpics(
          this.epics.fetchCriteria, this.epics.fetchSearchResults
        )
      )
    ];

    ngRedux.configureStore(
      rootReducer,
      InitialState,
      middleware,
      storeEnhancers
    );
  }
}
