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
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SelectionInfoComponent} from './selection-info/selection-info.component';
import {CriteriaTreeComponent} from './tree/tree.component';

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
    CriteriaTreeComponent,
    GenderChartComponent,
    DemographicsComponent,
    ModalComponent,
    ModifierPageComponent,
    OverviewComponent,
    SearchGroupListComponent,
    ListSearchComponent,
    SelectionInfoComponent,
    SafeHtmlPipe,
  ],
  providers: [TitleCasePipe]
})
export class CohortSearchModule {}
