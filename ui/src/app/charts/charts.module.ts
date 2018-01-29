import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ClarityModule} from '@clr/angular';

import ChartContainerComponent from './chart-container/component';
import DemographicsOverviewChartComponent from './demographics-overview/component';


@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
  ],
  declarations: [
    ChartContainerComponent,
    DemographicsOverviewChartComponent,
  ],
  exports: [
    DemographicsOverviewChartComponent,
  ]
})
export class ChartsModule {}
