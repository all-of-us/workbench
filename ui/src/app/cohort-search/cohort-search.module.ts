// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule} from '@angular-redux/store';

/* Components */
import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {
  ChartsComponent,
  GenderChartComponent,
  RaceChartComponent,
  GoogleChartComponent,
} from './charts';

/* Other Objects */
import {CriteriaWizardModule} from './criteria-wizard/criteria-wizard.module';
import {CohortSearchRouter} from './router.module';
import {
  CohortSearchActions,
  CohortSearchEpics,
  ConfigureStore,
} from './redux';
import {CohortBuilderService} from 'generated';

// tslint:enable:max-line-length

@NgModule({
  imports: [
    // Angular
    CommonModule,
    // 3rd Party
    ClarityModule,
    NgReduxModule,
    // Ours
    CohortSearchRouter,
    CriteriaWizardModule,
  ],
  declarations: [
    CohortBuilderComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    ChartsComponent,
    GenderChartComponent,
    RaceChartComponent,
    GoogleChartComponent,
  ],
  providers: [
    CohortBuilderService,
    CohortSearchActions,
    CohortSearchEpics,
    ConfigureStore,
  ]
})
export class CohortSearchModule {
  constructor(store: ConfigureStore) {}
}
