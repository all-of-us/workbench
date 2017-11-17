import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ClarityModule} from 'clarity-angular';

import {AnnotationsComponent} from './annotations/annotations.component';
import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {MedicationsComponent} from './medications/medications.component';
import {SubjectDetailComponent} from './subject-detail/subject-detail.component';
import {SubjectListComponent} from './subject-list/subject-list.component';

import {CohortReviewRouter} from './router.module';

@NgModule({
  imports: [
    ClarityModule,
    CohortReviewRouter,
    CommonModule,
  ],
  declarations: [
    AnnotationsComponent,
    CohortReviewComponent,
    MedicationsComponent,
    SubjectDetailComponent,
    SubjectListComponent,
  ]
})
export class CohortReviewModule {}
