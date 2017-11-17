import {NgReduxModule} from '@angular-redux/store';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ClarityModule} from 'clarity-angular';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupComponent} from './search-group/search-group.component';

/* Other Objects */
import {ChartsModule} from './charts/charts.module';
import {CriteriaWizardModule} from './criteria-wizard/criteria-wizard.module';
import {CohortSearchActions, CohortSearchEpics, ConfigureStore} from './redux';
import {CohortSearchRouter} from './router.module';

import {CohortBuilderService} from 'generated';

@NgModule({
  imports: [
    // Angular
    CommonModule,
    // 3rd Party
    ClarityModule,
    NgReduxModule,
    NgxPopperModule,
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
