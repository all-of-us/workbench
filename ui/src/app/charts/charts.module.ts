import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ClarityModule} from 'clarity-angular';

import ChartContainer from './chart-container/component';
import DemographicsOverviewChartComponent from './demographics-overview/component';


@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
  ],
  declarations: [
    ChartContainer,
    DemographicsOverviewChartComponent,
  ],
  exports: [
    DemographicsOverviewChartComponent,
  ]
})
export class ChartsModule {}
