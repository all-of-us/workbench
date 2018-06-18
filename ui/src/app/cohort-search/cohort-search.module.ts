import {NgReduxModule} from '@angular-redux/store';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {DemographicsComponent} from './demographics/demographics.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {ModalComponent} from './modal/modal.component';
import {ModifierPageComponent} from './modifier-page/modifier-page.component';
import {MultiSelectComponent} from './multi-select/multi-select.component';
import {NodeInfoComponent} from './node-info/node-info.component';
import {NodeComponent} from './node/node.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SelectionInfoComponent} from './selection-info/selection-info.component';
import {TreeComponent} from './tree/tree.component';

import {CohortCommonModule} from '../cohort-common/module';

/* Other Objects */
import {CohortSearchActions, CohortSearchEpics, ConfigureStore} from './redux';
import { AttributesPageComponent } from './attributes-page/attributes-page.component';
import { AttributesSelectComponent } from './attributes-select/attributes-select.component';


const routes: Routes = [{
  path: '',
  component: CohortSearchComponent,
  data: {title: 'Build Cohort Criteria'},
}];


@NgModule({
  imports: [
    // Angular
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    // 3rd Party
    ClarityModule,
    NgReduxModule,
    NgxChartsModule,
    NgxPopperModule,
    NouisliderModule,
    // Ours
    CohortCommonModule,
  ],
  declarations: [
    CohortSearchComponent,
    DemographicsComponent,
    GenderChartComponent,
    ModalComponent,
    ModifierPageComponent,
    MultiSelectComponent,
    NodeComponent,
    NodeInfoComponent,
    OverviewComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    SearchGroupListComponent,
    SearchGroupSelectComponent,
    SelectionInfoComponent,
    TreeComponent,
    AttributesPageComponent,
    AttributesSelectComponent,
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
