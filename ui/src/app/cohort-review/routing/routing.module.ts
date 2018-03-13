import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CreateReviewPage} from '../create-review-page/create-review-page';
import {DetailPage} from '../detail-page/detail-page';
import {OverviewPage} from '../overview-page/overview-page';
import {PageLayout} from '../page-layout/page-layout';
import {TablePage} from '../table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';
import {ParticipantAnnotationsResolver} from './participant-annotations.resolver';
import {ParticipantResolver} from './participant.resolver';

import {AnnotationDefinitionsResolver} from '../../resolvers/annotation-definitions';
import {CohortResolver} from '../../resolvers/cohort';
import {ReviewResolver} from '../../resolvers/review';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants'
  },
  resolve: {
    annotationDefinitions: AnnotationDefinitionsResolver,
    cohort: CohortResolver,
    review: ReviewResolver,
  },
  children: [{
    path: 'create',
    component: CreateReviewPage,
    data: {
      title: 'Create a New Cohort Review',
    },
  }, {
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
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AnnotationDefinitionsResolver,
    CohortResolver,
    DemographicConceptMapsResolver,
    ParticipantResolver,
    ParticipantAnnotationsResolver,
    ReviewResolver,
  ],
})
export class CohortReviewRoutingModule {}
