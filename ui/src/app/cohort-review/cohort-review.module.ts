/* tslint:disable:max-line-length */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ChartsModule} from '../charts/charts.module';

import {CohortReviewComponent} from './cohort-review/cohort-review.component';
import {CreateReviewComponent} from './create-review/create-review.component';
import {OverviewComponent} from './overview/overview.component';
import {ReviewNavComponent} from './review-nav/review-nav.component';

import {AnnotationItemComponent} from './annotation-item/annotation-item.component';
import {AnnotationListComponent} from './annotation-list/annotation-list.component';
import {SetAnnotationCreateComponent} from './set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from './set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from './set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from './set-annotation-modal/set-annotation-modal.component';

import {ChoiceFilterComponent} from './choice-filter/choice-filter.component';
import {ParticipantTableComponent} from './participant-table/participant-table.component';
import {StatusFilterComponent} from './status-filter/status-filter.component';

import {ParticipantDetailComponent} from './participant-detail/participant-detail.component';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';
import {SidebarContentComponent} from './sidebar-content/sidebar-content.component';

import {FullPageDirective} from './directives/fullPage.directive';
import {ReviewStateService} from './review-state.service';

import {CohortReviewRoutingModule} from './routing/routing.module';

import {WorkspacesService} from 'generated';
/* tslint:enable:max-line-length */


@NgModule({
  imports: [
    CohortReviewRoutingModule,
    ClarityModule,
    CommonModule,
    ReactiveFormsModule,
    ChartsModule,
  ],
  declarations: [
    /* Components - Layout & General Use */
    CohortReviewComponent,
    CreateReviewComponent,
    OverviewComponent,
    ReviewNavComponent,

    /* Annotations */
    AnnotationItemComponent,
    AnnotationListComponent,
    SetAnnotationCreateComponent,
    SetAnnotationItemComponent,
    SetAnnotationListComponent,
    SetAnnotationModalComponent,

    /* Participant Table */
    ChoiceFilterComponent,
    StatusFilterComponent,
    ParticipantTableComponent,

    /* Participant Detail */
    ParticipantDetailComponent,
    ParticipantStatusComponent,
    SidebarContentComponent,

    /* Directives */
    FullPageDirective,
  ],
  providers: [ReviewStateService]
})
export class CohortReviewModule {}
