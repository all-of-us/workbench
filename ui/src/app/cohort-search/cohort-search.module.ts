import {TitleCasePipe} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortCommonModule} from 'app/cohort-common/module';
import {NgxPopperModule} from 'ngx-popper';

/* Components */
import {CohortSearchComponent} from './cohort-search/cohort-search.component';
import {ModalComponent} from './modal/modal.component';
import {OverviewComponent} from './overview/overview.component';
import {SearchGroupListComponent} from './search-group-list/search-group-list.component';

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
    helpContentKey: 'cohortBuilder'
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
    // Ours
    CohortCommonModule,
  ],
  declarations: [
    CohortSearchComponent,
    ModalComponent,
    OverviewComponent,
    SearchGroupListComponent,
    SafeHtmlPipe,
  ],
  providers: [TitleCasePipe]
})
export class CohortSearchModule {}
