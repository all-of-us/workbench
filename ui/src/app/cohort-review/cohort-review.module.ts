/* tslint:disable:max-line-length */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {ChartsModule} from '../charts/charts.module';

import {AnnotationManagerComponent} from './annotation-manager/annotation-manager.component';
import {AnnotationsComponent} from './annotations/annotations.component';
import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {CreateReviewComponent} from './create-review/create-review.component';
import {OverviewComponent} from './overview/overview.component';
import {ParticipantAnnotationComponent} from './participant-annotation/participant-annotation.component';
import {ParticipantDetailComponent} from './participant-detail/participant-detail.component';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';
import {ParticipantTableComponent} from './participant-table/participant-table.component';
import {ReviewNavComponent} from './review-nav/review-nav.component';
import {SetAnnotationDetailComponent} from './set-annotation-detail/set-annotation-detail.component';
import {SetAnnotationMasterComponent} from './set-annotation-master/set-annotation-master.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {SidebarDirective} from './directives/sidebar.directive';
import {ReviewStateService} from './review-state.service';

import {AnnotationValuesResolver} from './guards/annotation-values-resolver.guard';
import {ParticipantResolver} from './guards/participant-resolver.guard';
/* tslint:enable:max-line-length */

import {WorkspacesService} from 'generated';

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
    }, {
      path: 'participants/:pid',
      component: ParticipantDetailComponent,
      resolve: {
        participant: ParticipantResolver,
        annotations: AnnotationValuesResolver,
      }
    }
  ],
}];

const components = [
  AnnotationManagerComponent,
  AnnotationsComponent,
  CohortReviewComponent,
  CreateReviewComponent,
  OverviewComponent,
  ParticipantAnnotationComponent,
  ParticipantDetailComponent,
  ParticipantStatusComponent,
  ParticipantTableComponent,
  ReviewNavComponent,
  SetAnnotationDetailComponent,
  SetAnnotationMasterComponent,
];

const directives = [
  FullPageDirective,
  SidebarDirective,
];

const services = [
  ReviewStateService,
];

const guards = [
  AnnotationValuesResolver,
  ParticipantResolver,
];

@NgModule({
  imports: [
    ClarityModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    ChartsModule,
  ],
  declarations: [
    ...components,
    ...directives,
  ],
  providers: [
    ...services,
    ...guards,
  ]
})
export class CohortReviewModule {}
