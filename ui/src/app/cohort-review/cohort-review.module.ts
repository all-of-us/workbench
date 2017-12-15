import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {CreateReviewComponent} from './create-review/create-review.component';
import {OverviewComponent} from './overview/overview.component';
import {ParticipantDetailComponent} from './participant-detail/participant-detail.component';
import {ParticipantPagerComponent} from './participant-pager/participant-pager.component';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {SidebarDirective} from './directives/sidebar.directive';
import {ReviewStateService} from './review-state.service';

import {CohortResolver} from '../guards/cohort-resolver.guard';
import {ReviewResolver} from '../guards/review-resolver.guard';


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
    cohort: CohortResolver,
    review: ReviewResolver,
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
    CreateReviewComponent,
    FullPageDirective,
    OverviewComponent,
    SidebarDirective,
    ParticipantDetailComponent,
    ParticipantPagerComponent,
    ParticipantStatusComponent,
  ],
  providers: [
    CohortResolver,
    ReviewResolver,
    CohortReviewService,
    ReviewStateService,
  ]
})
export class CohortReviewModule {}
