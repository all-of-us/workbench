import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {OverviewComponent} from './overview/overview.component';
import {SubjectDetailComponent} from './subject-detail/subject-detail.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {SidebarDirective} from './directives/sidebar.directive';

import {CohortResolverGuard} from './guards/cohort-resolver.guard';
import {ReviewResolverGuard} from './guards/review-resolver.guard';

import {CohortReviewService} from 'generated';

const routes = [{
  path: 'workspace/:ns/:wsid/cohorts/:cid/review',
  component: CohortReviewComponent,
  data: {title: 'Review Cohort Subjects'},
  children: [
    {path: '', redirectTo: 'overview', pathMatch: 'full'},
    {path: 'overview', component: OverviewComponent},
    {path: ':subjectID', component: SubjectDetailComponent},
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
    OverviewComponent,
    SubjectDetailComponent,
    FullPageDirective,
    SidebarDirective,
  ],
  providers: [
    CohortResolverGuard,
    ReviewResolverGuard,
    CohortReviewService,
  ]
})
export class CohortReviewModule {}
