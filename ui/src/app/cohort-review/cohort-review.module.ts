import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {SubjectListComponent} from './subject-list/subject-list.component';
import {SubjectDetailComponent} from './subject-detail/subject-detail.component';
import {AnnotationsComponent} from './annotations/annotations.component';
import {MedicationsComponent} from './medications/medications.component';

import {CohortReviewRouter} from './router.module';

@NgModule({
  imports: [
    CommonModule,
    CohortReviewRouter,
    ClarityModule
  ],
  declarations: [
    CohortReviewComponent,
    SubjectListComponent,
    SubjectDetailComponent,
    AnnotationsComponent,
    MedicationsComponent
  ]
})
export class CohortReviewModule { }
