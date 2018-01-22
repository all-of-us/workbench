import {NgModule} from '@angular/core';

import ChartContainer from './chart-container/component';
import DemographicsOverviewChartComponent from './demographics-overview/component';


@NgModule({
  declarations: [
    ChartContainer,
    DemographicsOverviewChartComponent,
  ],
  exports: [
    DemographicsOverviewChartComponent,
  ]
})
export class ChartsModule {}
