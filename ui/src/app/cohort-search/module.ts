// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule, NgRedux, DevToolsExtension} from '@angular-redux/store';

/* Components */
import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {RaceChartComponent} from './race-chart/race-chart.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchResultComponent} from './search-result/search-result.component';
import {WizardCriteriaGroupComponent} from './wizard-criteria-group/wizard-criteria-group.component';
import {WizardModalComponent} from './wizard-modal/wizard-modal.component';
import {WizardTreeParentComponent} from './wizard-tree-parent/wizard-tree-parent.component';
import {WizardTreeChildrenComponent} from './wizard-tree-children/wizard-tree-children.component';
import {WizardSelectComponent} from './wizard-select/wizard-select.component';

/* Other Objects */
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
    CohortBuilderComponent,
    GenderChartComponent,
    GoogleChartDirective,
    RaceChartComponent,
    SearchGroupComponent,
    SearchResultComponent,
    WizardSelectComponent,
    WizardModalComponent,
    WizardTreeParentComponent,
    WizardTreeChildrenComponent,
    WizardCriteriaGroupComponent,
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    BroadcastService,
    CohortBuilderService,
    CohortSearchActions
  ]
})
export class CohortSearchModule {
  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private devTools: DevToolsExtension) {

    let storeEnhancers = [];
    if (environment.debug && devTools.isEnabled()) {
      storeEnhancers = [...storeEnhancers, devTools.enhancer()];
    }

    ngRedux.configureStore(
      rootReducer,
      InitialState,
      [],
      storeEnhancers
    );
  }
}
