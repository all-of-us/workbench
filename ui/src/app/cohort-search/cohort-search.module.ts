import {NgReduxModule} from '@angular-redux/store';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortCommonModule} from 'app/cohort-common/module';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {AttributesPageComponent} from './attributes-page/attributes-page.component';
import {CodeDropdownComponent} from './code-dropdown/code-dropdown.component';
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {DemographicsComponent} from './demographics/demographics.component';
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
import {ModalComponent} from './modal/modal.component';
import {ModifierPageComponent} from './modifier-page/modifier-page.component';
import {MultiSelectComponent} from './multi-select/multi-select.component';
import {NodeInfoComponent} from './node-info/node-info.component';
import {NodeComponent} from './node/node.component';
import {OptionInfoComponent} from './option-info/option-info.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchBarComponent} from './search-bar/search-bar.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from './search-group-select/search-group-select.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SelectionInfoComponent} from './selection-info/selection-info.component';
import {TreeComponent} from './tree/tree.component';

import {BreadcrumbType} from 'app/utils/navigation';

/* Other Objects */
import {CohortSearchActions, CohortSearchEpics, ConfigureStore} from './redux';
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
    NgReduxModule,
    NgxPopperModule,
    NouisliderModule,
    // Ours
    CohortCommonModule,
  ],
  declarations: [
    CohortSearchComponent,
    CodeDropdownComponent,
    DemographicsComponent,
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
    ModalComponent,
    ModifierPageComponent,
    MultiSelectComponent,
    NodeComponent,
    NodeInfoComponent,
    OptionInfoComponent,
    OverviewComponent,
    SearchBarComponent,
    SearchGroupComponent,
    SearchGroupItemComponent,
    SearchGroupListComponent,
    SearchGroupSelectComponent,
    SelectionInfoComponent,
    TreeComponent,
    AttributesPageComponent,
    SafeHtmlPipe,
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
