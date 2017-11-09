import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {ChartsComponent} from './charts.component';
import {GenderChartComponent} from './gender-chart.component';
import {RaceChartComponent} from './race-chart.component';

@NgModule({
  imports: [
    CommonModule,
    NgxChartsModule,
  ],
  exports: [
    ChartsComponent
  ],
  declarations: [
    ChartsComponent,
    GenderChartComponent,
    RaceChartComponent,
  ]
})
export class ChartsModule {}
