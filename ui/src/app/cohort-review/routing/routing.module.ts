import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPage} from '../detail-page/detail-page';
import {OverviewPage} from '../overview-page/overview-page';
import {PageLayout} from '../page-layout/page-layout';
import {TablePage} from '../table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';
import {ParticipantAnnotationsResolver} from './participant-annotations.resolver';
import {ParticipantResolver} from './participant.resolver';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {title: 'Review Cohort Participants'},
  children: [
    {
      path: '',
      redirectTo: 'overview',
      pathMatch: 'full',
    }, {
      path: 'overview',
      component: OverviewPage,
    }, {
      path: 'participants',
      component: TablePage,
      resolve: {
        concepts: DemographicConceptMapsResolver,
      }
    }, {
      path: 'participants/:pid',
      component: DetailPage,
      resolve: {
        participant: ParticipantResolver,
        annotations: ParticipantAnnotationsResolver,
      }
    }
  ],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DemographicConceptMapsResolver,
    ParticipantResolver,
    ParticipantAnnotationsResolver,
  ],
})
export class CohortReviewRoutingModule {}
