/* tslint:disable:max-line-length */
import {NgReduxModule} from '@angular-redux/store';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import { ChartModule } from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import * as highCharts from 'highcharts';
import {NgxPopperModule} from 'ngx-popper';

/* Pages */
import {CohortCommonModule} from 'app/cohort-common/module';
// This is to get cohortsaerchstore acces, might need to change
import {AddAnnotationDefinitionModalComponent, EditAnnotationDefinitionsModalComponent} from './annotation-definition-modals/annotation-definition-modals.component';
import {AnnotationItemComponent} from './annotation-item/annotation-item.component';
import {AnnotationListComponent} from './annotation-list/annotation-list.component';
import {ClearButtonFilterComponent} from './clearbutton-filter/clearbutton-filter.component';
import {ClearButtonInMemoryFilterComponent} from './clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {CreateReviewPage} from './create-review-page/create-review-page';
// This is a temporary measure until we have specs and APIs for overview specific charts
import {DetailHeaderComponent} from './detail-header/detail-header.component';
import {DetailPage} from './detail-page/detail-page';
import {DetailTabTableComponent} from './detail-tab-table/detail-tab-table.component';
import {DetailTabsComponent} from './detail-tabs/detail-tabs.component';
import {IndividualParticipantsChartsComponent} from './individual-participants-charts/individual-participants-charts';
import {MultiSelectFilterComponent} from './multiselect-filter/multiselect-filter.component';
import {OverviewPage} from './overview-page/overview-page';
import {PageLayout} from './page-layout/page-layout';
import {ParticipantStatusComponent} from './participant-status/participant-status.component';
import {ParticipantsChartsComponent} from './participants-charts/participant-charts';
import {QueryCohortDefinitionComponent} from './query-cohort-definition/query-cohort-definition.component';
import {QueryDescriptiveStatsComponent} from './query-descriptive-stats/query-descriptive-stats.component';
import {QueryReportComponent} from './query-report/query-report.component';
import {CohortReviewRoutingModule} from './routing/routing.module';
import {SidebarContentComponent} from './sidebar-content/sidebar-content.component';
import {StatusFilterComponent} from './status-filter/status-filter.component';
import {TablePage} from './table-page/table-page';


/* tslint:enable:max-line-length */

@NgModule({
  imports: [
    // Angular
    NgReduxModule,
    CommonModule,
    ReactiveFormsModule,
    // Routes
    CohortReviewRoutingModule,
    // 3rd Party
    ClarityModule,
    NgxChartsModule,
    NgxPopperModule,
    ChartModule,
    // Ours
    CohortCommonModule,

  ],
  declarations: [
    /* Scaffolding and Pages */
    CreateReviewPage,
    DetailPage,
    OverviewPage,
    PageLayout,
    QueryReportComponent,
    TablePage,

    /* Annotations */
    AnnotationItemComponent,
    AnnotationListComponent,
    AddAnnotationDefinitionModalComponent,
    EditAnnotationDefinitionsModalComponent,

    /* Participant Table */
    ClearButtonFilterComponent,
    MultiSelectFilterComponent,
    StatusFilterComponent,
    ClearButtonInMemoryFilterComponent,
    IndividualParticipantsChartsComponent,
    /* Participant Detail */
    ParticipantStatusComponent,
    SidebarContentComponent,
    DetailHeaderComponent,
    DetailTabsComponent,
    DetailTabTableComponent,
    ParticipantsChartsComponent,
    QueryCohortDefinitionComponent,
    QueryDescriptiveStatsComponent
  ],
  providers: [
    {
      provide: HighchartsStatic,
      useValue: highCharts
    },
  ]
})
export class CohortReviewModule {}
