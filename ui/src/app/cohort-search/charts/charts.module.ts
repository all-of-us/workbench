import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {GoogleChartDirective} from './google-chart.directive';
import {ChartsComponent} from './charts.component';

@NgModule({
  imports: [
    CommonModule,
    NgxChartsModule,
  ],
  exports: [
    ChartsComponent
  ],
  declarations: [
    GoogleChartDirective,
    ChartsComponent,
  ]
})
export class ChartsModule {}
