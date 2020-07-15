import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CohortCommonModule} from 'app/cohort-common/module';

/* Components */
import {CohortPageComponent} from './cohort-page/cohort-page.component';
import {CohortSearchComponent} from './cohort-search/cohort-search.component';

import {BreadcrumbType} from 'app/utils/navigation';

/* Other Objects */
import {CanDeactivateGuard} from 'app/guards/can-deactivate-guard.service';


const routes: Routes = [{
  path: '',
  component: CohortPageComponent,
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
    RouterModule.forChild(routes),
    // Ours
    CohortCommonModule,
  ],
  declarations: [
    CohortPageComponent,
    CohortSearchComponent,
  ],
  providers: []
})
export class CohortSearchModule {}
