import {TitleCasePipe} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortCommonModule} from 'app/cohort-common/module';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {AttributesPageComponent} from './attributes-page/attributes-page.component';
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {DemographicsComponent} from './demographics/demographics.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {ListSearchComponent} from './list-search/list-search.component';
import {ModalComponent} from './modal/modal.component';
import {ModifierPageComponent} from './modifier-page/modifier-page.component';
import {NodeInfoComponent} from './node-info/node-info.component';
import {NodeComponent} from './node/node.component';
import {OptionInfoComponent} from './option-info/option-info.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchBarComponent} from './search-bar/search-bar.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SelectionInfoComponent} from './selection-info/selection-info.component';
import {TreeComponent} from './tree/tree.component';

import {BreadcrumbType} from 'app/utils/navigation';

/* Other Objects */
import {CanDeactivateGuard} from 'app/guards/can-deactivate-guard.service';
import {SafeHtmlPipe} from './safe-html.pipe';


const routes: Routes = [{
  path: '',
  component: CohortSearchComponent,
  canDeactivate: [CanDeactivateGuard],
  data: {
    title: 'Build Cohort Criteria',
    breadcrumb: BreadcrumbType.CohortAdd,
    helpContent: 'cohortBuilder'
  },
}];


@NgModule({
  imports: [
    // Angular
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    // 3rd Party
    ClarityModule,
    NgxPopperModule,
    NouisliderModule,
    // Ours
    CohortCommonModule,
  ],
  declarations: [
    AttributesPageComponent,
    CohortSearchComponent,
    GenderChartComponent,
    DemographicsComponent,
    ModalComponent,
    ModifierPageComponent,
    NodeComponent,
    NodeInfoComponent,
    OptionInfoComponent,
    OverviewComponent,
    SearchBarComponent,
    SearchGroupListComponent,
    SearchGroupComponent,
    ListSearchComponent,
    SelectionInfoComponent,
    TreeComponent,
    SafeHtmlPipe,
    SearchGroupSelectComponent,
  ],
  providers: [TitleCasePipe]
})
export class CohortSearchModule {}
