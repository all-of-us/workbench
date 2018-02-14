/* tslint:disable:max-line-length */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ChartsModule} from '../charts/charts.module';

import {AnnotationManagerComponent} from './annotation-manager/annotation-manager.component';
import {AnnotationsComponent} from './annotations/annotations.component';
import {ChoiceFilterComponent} from './choice-filter/choice-filter.component';
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

import {CohortReviewRoutingModule} from './routing/routing.module';
/* tslint:enable:max-line-length */

import {WorkspacesService} from 'generated';
import { StatusFilterComponent } from './status-filter/status-filter.component';

@NgModule({
  imports: [
    CohortReviewRoutingModule,
    ClarityModule,
    CommonModule,
    ReactiveFormsModule,
    ChartsModule,
  ],
  declarations: [
    /* Components */
    AnnotationManagerComponent,
    AnnotationsComponent,
    ChoiceFilterComponent,
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

    /* Directives */
    FullPageDirective,
    SidebarDirective,
    StatusFilterComponent,
  ],
  providers: [ReviewStateService]
})
export class CohortReviewModule {}
