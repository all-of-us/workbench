import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortCommonModule} from 'app/cohort-common/module';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {GenderChartComponent} from './gender-chart/gender-chart.component';
import {ListAttributesPageComponent} from './list-attributes-page/list-attributes-page.component';
import {ListDemographicsComponent} from './list-demographics/list-demographics.component';
import {ListModalComponent} from './list-modal/list-modal.component';
import {ListModifierPageComponent} from './list-modifier-page/list-modifier-page.component';
import {ListNodeInfoComponent} from './list-node-info/list-node-info.component';
import {ListNodeComponent} from './list-node/list-node.component';
import {ListOptionInfoComponent} from './list-option-info/list-option-info.component';
import {ListOverviewComponent} from './list-overview/list-overview.component';
import {ListSearchBarComponent} from './list-search-bar/list-search-bar.component';
import {ListSearchGroupItemComponent} from './list-search-group-item/list-search-group-item.component';
import {ListSearchGroupListComponent} from './list-search-group-list/list-search-group-list.component';
import {ListSearchGroupComponent} from './list-search-group/list-search-group.component';
import {ListSearchComponent} from './list-search/list-search.component';
import {ListSelectionInfoComponent} from './list-selection-info/list-selection-info.component';
import {ListTreeComponent} from './list-tree/list-tree.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';

import {BreadcrumbType} from 'app/utils/navigation';

/* Other Objects */
import {SafeHtmlPipe} from './safe-html.pipe';


const routes: Routes = [{
  path: '',
  component: CohortSearchComponent,
  data: {
    title: 'Build Cohort Criteria',
    breadcrumb: BreadcrumbType.CohortAdd
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
    CohortSearchComponent,
    GenderChartComponent,
    ListAttributesPageComponent,
    ListDemographicsComponent,
    ListModalComponent,
    ListModifierPageComponent,
    ListNodeComponent,
    ListNodeInfoComponent,
    ListOptionInfoComponent,
    ListOverviewComponent,
    ListSearchBarComponent,
    ListSearchGroupItemComponent,
    ListSearchGroupListComponent,
    ListSearchGroupComponent,
    ListSearchComponent,
    ListSelectionInfoComponent,
    ListTreeComponent,
    SafeHtmlPipe,
    SearchGroupSelectComponent,
  ],
})
export class CohortSearchModule {}
