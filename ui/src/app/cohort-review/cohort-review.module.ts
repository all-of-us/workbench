/* tslint:disable:max-line-length */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ChartsModule} from '../charts/charts.module';

/* Pages */
import {DetailPage} from './detail-page/detail-page';
import {OverviewPage} from './overview-page/overview-page';
import {PageLayout} from './page-layout/page-layout';
import {TablePage} from './table-page/table-page';

import {CreateReviewComponent} from './create-review/create-review.component';
import {ReviewNavComponent} from './review-nav/review-nav.component';

import {AnnotationItemComponent} from './annotation-item/annotation-item.component';
import {AnnotationListComponent} from './annotation-list/annotation-list.component';
import {SetAnnotationCreateComponent} from './set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from './set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from './set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from './set-annotation-modal/set-annotation-modal.component';

import {ChoiceFilterComponent} from './choice-filter/choice-filter.component';
import {StatusFilterComponent} from './status-filter/status-filter.component';

import {ParticipantStatusComponent} from './participant-status/participant-status.component';
import {SidebarContentComponent} from './sidebar-content/sidebar-content.component';

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
    /* Scaffolding and Pages */
    DetailPage,
    OverviewPage,
    PageLayout,
    TablePage,

    CreateReviewComponent,
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

    /* Participant Detail */
    ParticipantStatusComponent,
    SidebarContentComponent,
  ],
  providers: [ReviewStateService]
})
export class CohortReviewModule {}
