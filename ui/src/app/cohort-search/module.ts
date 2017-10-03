// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule, NgRedux, DevToolsExtension} from '@angular-redux/store';
import {createEpicMiddleware} from 'redux-observable';

/* Components */
import {AddCriteriaComponent} from './add-criteria.component';
import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';
import {CriteriaTreeRootComponent} from './criteria-tree/root.component';
import {CriteriaTreeNodeComponent} from './criteria-tree/node.component';
import {NodeInfoComponent} from './criteria-tree/node-info.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {RaceChartComponent} from './race-chart/race-chart.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchResultComponent} from './search-result/search-result.component';
import {WizardCriteriaGroupComponent} from './wizard-criteria-group/wizard-criteria-group.component';
import {WizardModalComponent} from './wizard-modal/wizard-modal.component';

/* Other Objects */
import {Epics} from './epics';
import {environment} from 'environments/environment';
import {BroadcastService} from './broadcast.service';
import {CohortSearchState, InitialState, rootReducer} from './store';
import {CohortSearchRouter} from './router';
import {CohortSearchActions} from './actions';
import {GoogleChartDirective} from './google-chart.directive';

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
    GenderChartComponent,
    GoogleChartDirective,
    RaceChartComponent,
    SearchGroupComponent,
    SearchResultComponent,
    WizardModalComponent,
    WizardCriteriaGroupComponent,
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    BroadcastService,
    CohortBuilderService,
    CohortSearchActions,
    Epics,
  ]
})
export class CohortSearchModule {
  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private epics: Epics,
              private devTools: DevToolsExtension) {

    let storeEnhancers = [];
    if (environment.debug && devTools.isEnabled()) {
      storeEnhancers = [...storeEnhancers, devTools.enhancer()];
    }

    const middleware = [
      createEpicMiddleware(this.epics.fetchCriteria)
    ];

    ngRedux.configureStore(
      rootReducer,
      InitialState,
      middleware,
      storeEnhancers
    );
  }
}
