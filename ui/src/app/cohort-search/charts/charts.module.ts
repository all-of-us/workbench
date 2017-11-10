import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';

import {GoogleChartDirective} from './google-chart.directive';
import {ChartsComponent} from './charts.component';

@NgModule({
  imports: [
    CommonModule,
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
