import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';

import {CohortReviewRouter} from './router.module';

@NgModule({
  imports: [
    ClarityModule,
    CohortReviewRouter,
    CommonModule,
  ],
  declarations: [
    CohortReviewComponent,
  ]
})
export class CohortReviewModule {}
