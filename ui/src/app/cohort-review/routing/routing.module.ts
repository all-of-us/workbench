import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPage} from 'app/cohort-review/detail-page/detail-page';
import {PageLayout} from 'app/cohort-review/page-layout/page-layout';
import {TablePage} from 'app/cohort-review/table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';

import {ReviewResolver} from 'app/resolvers/review';
import {BreadcrumbType} from 'app/utils/navigation';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants',
    breadcrumb: BreadcrumbType.Cohort
  },
  resolve: {
    review: ReviewResolver,
  },
  children: [{
    path: 'participants',
    component: TablePage,
    resolve: {
      concepts: DemographicConceptMapsResolver,
    },
    data: {
      breadcrumb: BreadcrumbType.Cohort
    }
  }, {
    path: 'participants/:pid',
    component: DetailPage,
    data: {
      breadcrumb: BreadcrumbType.Participant
    }
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DemographicConceptMapsResolver,
    ReviewResolver,
  ],
})
export class CohortReviewRoutingModule {}
