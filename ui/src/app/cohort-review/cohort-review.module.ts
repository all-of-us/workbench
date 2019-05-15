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
import {CreateReviewPage} from './create-review-page/create-review-page';
// This is a temporary measure until we have specs and APIs for overview specific charts
import {DetailHeaderComponent} from './detail-header/detail-header.component';
import {DetailPageComponent} from './detail-page/detail-page';
import {DetailTabsComponent} from './detail-tabs/detail-tabs.component';
import {PageLayout} from './page-layout/page-layout';
import {CohortReviewRoutingModule} from './routing/routing.module';
import {SidebarContentComponent} from './sidebar-content/sidebar-content.component';
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
    DetailPageComponent,
    PageLayout,
    TablePage,

    /* Annotations */
    AddAnnotationDefinitionModalComponent,
    EditAnnotationDefinitionsModalComponent,

    /* Participant Table */
    /* Participant Detail */
    SidebarContentComponent,
    DetailHeaderComponent,
    DetailTabsComponent,
  ],
  providers: [
    {
      provide: HighchartsStatic,
      useValue: highCharts
    },
  ]
})
export class CohortReviewModule {}
