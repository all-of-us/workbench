import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';

@NgModule({
  imports: [RouterModule.forChild([
    /* Define routes here */
    {
      path: 'workspace/:ns/:wsid/cohorts/:cid/build',
      component: CohortReviewComponent,
      data: {title: 'Edit Cohort Criteria'}
    }
  ])],
  exports: [RouterModule]
})
export class CohortReviewRouter {}
