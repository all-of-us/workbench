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

import {ChartComponent} from './chart/chart.component';

import {LocalStorageModule} from 'angular-2-local-storage';

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
  ],
  declarations: [
    ChartComponent,
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
