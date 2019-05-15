import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPageComponent} from 'app/cohort-review/detail-page/detail-page';
import {PageLayout} from 'app/cohort-review/page-layout/page-layout';
import {TablePage} from 'app/cohort-review/table-page/table-page';

import {BreadcrumbType} from 'app/utils/navigation';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants',
    breadcrumb: BreadcrumbType.Cohort
  },
  children: [{
    path: 'participants',
    component: TablePage,
    data: {
      breadcrumb: BreadcrumbType.Cohort
    }
  }, {
    path: 'participants/:pid',
    component: DetailPageComponent,
    data: {
      breadcrumb: BreadcrumbType.Participant,
      shouldReuse: true
    }
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [],
})
export class CohortReviewRoutingModule {}
