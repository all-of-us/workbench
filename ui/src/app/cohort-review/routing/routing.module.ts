import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {DetailPage} from '../detail-page/detail-page';
import {PageLayout} from '../page-layout/page-layout';
import {TablePage} from '../table-page/table-page';

import {DemographicConceptMapsResolver} from './demographic-concept-maps.resolver';
import {ParticipantAnnotationsResolver} from './participant-annotations.resolver';
import {ParticipantResolver} from './participant.resolver';

import {AnnotationDefinitionsResolver} from '../../resolvers/annotation-definitions';
import {ReviewResolver} from '../../resolvers/review';


const routes: Routes = [{
  path: '',
  component: PageLayout,
  data: {
    title: 'Review Cohort Participants'
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
          breadcrumb: {
            value: 'Participants',
            intermediate: true
          },
      }
  }, {
    path: 'participants/:pid',
    component: DetailPage,
    resolve: {
      annotationDefinitions: AnnotationDefinitionsResolver,
      participant: ParticipantResolver,
      annotations: ParticipantAnnotationsResolver,
    },
    data: {
      breadcrumb: {
        value: 'Participant :pid',
        intermediate: false
      }
    }
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AnnotationDefinitionsResolver,
    DemographicConceptMapsResolver,
    ParticipantResolver,
    ParticipantAnnotationsResolver,
    ReviewResolver,
  ],
})
export class CohortReviewRoutingModule {}
