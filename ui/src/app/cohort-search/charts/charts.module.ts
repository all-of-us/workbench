import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';

import {ChartsComponent} from './charts.component';
import {GoogleChartDirective} from './google-chart.directive';

@NgModule({
  imports: [
    CommonModule,
  ],
  exports: [
    ChartsComponent
  ],
  declarations: [
    ChartsComponent,
    GoogleChartDirective,
  ]
})
export class ChartsModule {}
