import {environment} from 'environments/environment';

import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http, HttpModule} from '@angular/http';
import {ClarityModule} from '@clr/angular';

/* Components */
import {ChartModule} from 'angular2-highcharts';
import {HighchartsStatic} from 'angular2-highcharts/dist/HighchartsService';
import * as highcharts from 'highcharts';
import 'highcharts/highcharts-more';


import {LocalStorageModule} from 'angular-2-local-storage';
import {ChartComponent} from './chart/chart.component';
import {ConceptChartsComponent} from './concept-charts/concept-charts.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ChartModule,
    HttpModule,
    ClarityModule,
    LocalStorageModule
  ],
  exports: [
    ChartComponent,
    ConceptChartsComponent
  ],
  declarations: [
    ChartComponent,
    ConceptChartsComponent
  ],
  providers: [
      {
        provide: HighchartsStatic,
        useValue: highcharts,
      },
  ]
})

export class DataBrowserModule {
  constructor() {}
}
