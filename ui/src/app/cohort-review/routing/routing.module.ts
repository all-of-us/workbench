import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortReviewComponent} from '../cohort-review/cohort-review.component';
import {OverviewComponent} from '../overview/overview.component';
import {ParticipantDetailComponent} from '../participant-detail/participant-detail.component';
import {ParticipantTableComponent} from '../participant-table/participant-table.component';

import {ParticipantAnnotationsResolver} from './participant-annotations.resolver';
import {ParticipantPageResolver} from './participant-page.resolver';
import {ParticipantResolver} from './participant.resolver';


const routes: Routes = [{
  path: '',
  component: CohortReviewComponent,
  data: {title: 'Review Cohort Participants'},
  children: [
    {
      path: '',
      redirectTo: 'overview',
      pathMatch: 'full',
    }, {
      path: 'overview',
      component: OverviewComponent,
    }, {
      path: 'participants',
      component: ParticipantTableComponent,
      resolve: {
        participants: ParticipantPageResolver,
      },
      runGuardsAndResolvers: 'paramsOrQueryParamsChange',
    }, {
      path: 'participants/:pid',
      component: ParticipantDetailComponent,
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
    ParticipantResolver,
    ParticipantAnnotationsResolver,
    ParticipantPageResolver,
  ],
})
export class CohortReviewRoutingModule {}
