import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';

import {ChartsComponent} from './charts.component';
import {PlotlyComponent} from './plotly.component';

@NgModule({
  imports: [
    CommonModule,
  ],
  exports: [
    ChartsComponent
  ],
  declarations: [
    ChartsComponent,
    PlotlyComponent,
  ]
})
export class ChartsModule {}
