import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

/* Components */
import {CohortPageComponent} from './cohort-page/cohort-page.component';

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
  imports: [RouterModule.forChild(routes)],
  declarations: [CohortPageComponent],
  providers: []
})
export class CohortSearchModule {}
