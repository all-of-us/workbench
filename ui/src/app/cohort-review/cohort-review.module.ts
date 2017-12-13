import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {OverviewComponent} from './overview/overview.component';
import {ParticipantDetailComponent} from './participant-detail/participant-detail.component';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {SidebarDirective} from './directives/sidebar.directive';

import {CohortResolverGuard} from './guards/cohort-resolver.guard';
import {ReviewResolverGuard} from './guards/review-resolver.guard';

import {CohortReviewService} from 'generated';

const routes = [{
  path: 'workspace/:ns/:wsid/cohorts/:cid/review',
  component: CohortReviewComponent,
  data: {title: 'Review Cohort Participants'},
  children: [
    {path: '', redirectTo: 'overview', pathMatch: 'full'},
    {path: 'overview', component: OverviewComponent},
    {path: ':participantId', component: ParticipantDetailComponent},
  ],
  resolve: {
    cohort: CohortResolverGuard,
    review: ReviewResolverGuard,
  }
}];

@NgModule({
  imports: [
    ClarityModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
  ],
  declarations: [
    CohortReviewComponent,
    FullPageDirective,
    OverviewComponent,
    SidebarDirective,
    ParticipantDetailComponent,
    ParticipantStatusComponent,
  ],
  providers: [
    CohortResolverGuard,
    ReviewResolverGuard,
    CohortReviewService,
  ]
})
export class CohortReviewModule {}
