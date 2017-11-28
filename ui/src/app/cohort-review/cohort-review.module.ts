import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';

import {CohortReviewRouter} from './router.module';

const routes = [{
    path: 'workspace/:ns/:wsid/cohorts/:cid/review',
    component: CohortReviewComponent,
    data: {title: 'Review Cohort Subjects'}
  }, {
    path: 'workspace/:ns/:wsid/cohorts/:cid/review/:subjectid',
    component: CohortReviewComponent,
    data: {title: 'Review Cohort Subjects'}
}];

@NgModule({
  imports: [
    ClarityModule,
    CohortReviewRouter,
    CommonModule,
    RouterModule.forChild(routes),
  ],
  declarations: [
    CohortReviewComponent,
  ]
})
export class CohortReviewModule {}
