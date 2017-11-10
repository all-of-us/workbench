// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule} from '@angular-redux/store';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';

/* Other Objects */
import {ChartsModule} from './charts/charts.module';
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
    ChartsModule,
    CriteriaWizardModule,
  ],
  declarations: [
    CohortSearchComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    SearchGroupListComponent,
    OverviewComponent,
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
