import {NgModule} from '@angular/core';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {CohortResolver} from '../resolvers/cohort';
import {ComboChartComponent} from './combo-chart/combo-chart.component';

import {ApiModule} from 'generated';

@NgModule({
  imports: [
    ApiModule,
    NgxChartsModule,
  ],
  declarations: [
    ComboChartComponent,
  ],
  exports: [
    // TODO: This could be moved back to CohortSearchModule once no longer
    // needed in CohortReviewModule.
    ComboChartComponent
  ],
  providers: [
    CohortResolver
  ]
})
export class CohortCommonModule {}
