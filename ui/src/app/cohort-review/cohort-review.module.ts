import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {ReviewResolverGuard} from './guards';
import {OverviewComponent} from './overview/overview.component';
import {SubjectDetailComponent} from './subject-detail/subject-detail.component';

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
  resolve: { review: ReviewResolverGuard}
}];

@NgModule({
  imports: [
    ClarityModule,
    CommonModule,
    RouterModule.forChild(routes),
  ],
  declarations: [
    CohortReviewComponent,
    OverviewComponent,
    SubjectDetailComponent,
  ],
  providers: [
    ReviewResolverGuard,
    CohortReviewService,
  ]
})
export class CohortReviewModule {}
