import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';

import {ChartsComponent} from './charts.component';
import {GenderChartComponent} from './gender-chart.component';
import {RaceChartComponent} from './race-chart.component';
import {GoogleChartComponent} from './google-chart.component';

@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
  ],
  exports: [ChartsComponent],
  declarations: [
    ChartsComponent,
    GenderChartComponent,
    RaceChartComponent,
    GoogleChartComponent,
  ]
})
export class ChartsModule {}
