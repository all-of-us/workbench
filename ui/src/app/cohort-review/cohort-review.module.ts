/* tslint:disable:max-line-length */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {NgxPopperModule} from 'ngx-popper';

import {ChartsModule} from '../charts/charts.module';

/* Pages */
import {CreateReviewPage} from './create-review-page/create-review-page';
import {DetailPage} from './detail-page/detail-page';
import {OverviewPage} from './overview-page/overview-page';
import {PageLayout} from './page-layout/page-layout';
import {TablePage} from './table-page/table-page';

import {ReviewNavComponent} from './review-nav/review-nav.component';

import {DetailConditionsComponent} from './detail-conditions/detail-conditions.component';
import {DetailHeaderComponent} from './detail-header/detail-header.component';
import {DetailTabsComponent} from './detail-tabs/detail-tabs.component';

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
    // Angular
    CommonModule,
    ReactiveFormsModule,
    // Routes
    CohortReviewRoutingModule,
    // 3rd Party
    ClarityModule,
    NgxChartsModule,
    NgxPopperModule,
    // Ours
    ChartsModule,
  ],
  declarations: [
    /* Scaffolding and Pages */
    CreateReviewPage,
    DetailPage,
    OverviewPage,
    PageLayout,
    TablePage,

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
    DetailHeaderComponent,
    DetailTabsComponent,
    DetailConditionsComponent,
  ],
  providers: [ReviewStateService]
})
export class CohortReviewModule {}
