import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPage} from 'app/cohort-review/detail-page/detail-page';
import {PageLayout} from 'app/cohort-review/page-layout/page-layout';
import {TablePage} from 'app/cohort-review/table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';

import {QueryReportComponent} from 'app/cohort-review/query-report/query-report.component';
import {ReviewResolver} from 'app/resolvers/review';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants',
    breadcrumb: 'cohort'
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
      breadcrumb: 'cohort'
    }
  }, {
    path: 'participants/:pid',
    component: DetailPage,
    data: {
      breadcrumb: 'participant'
    }
  }, {
    path: 'report',
    component: QueryReportComponent,
    data: {
      breadcrumb: 'report'
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
