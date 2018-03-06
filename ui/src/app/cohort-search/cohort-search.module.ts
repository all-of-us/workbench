import {NgReduxModule} from '@angular-redux/store';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';
import {SearchGroupComponent} from './search-group/search-group.component';

/* Other Objects */
import {ChartsModule} from './charts/charts.module';
import {CriteriaWizardModule} from './criteria-wizard/criteria-wizard.module';
import {CohortSearchActions, CohortSearchEpics, ConfigureStore} from './redux';

const routes: Routes = [{
  path: '',
  component: CohortSearchComponent,
  data: {title: 'Build Cohort Criteria'},
}];

@NgModule({
  imports: [
    // Angular
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    // 3rd Party
    ClarityModule,
    NgReduxModule,
    NgxPopperModule,
    // Ours
    ChartsModule,
    CriteriaWizardModule,
  ],
  declarations: [
    CohortSearchComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    SearchGroupListComponent,
    OverviewComponent,
    SearchGroupSelectComponent,
  ],
  providers: [
    CohortSearchActions,
    CohortSearchEpics,
    ConfigureStore,
  ]
})
export class CohortSearchModule {
  constructor(store: ConfigureStore) {}
}
