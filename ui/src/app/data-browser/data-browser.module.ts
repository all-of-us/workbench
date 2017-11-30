
import { BrowserModule } from '@angular/platform-browser';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { HttpModule } from '@angular/http';
import { ClarityModule } from 'clarity-angular';
import {CommonModule} from '@angular/common';

/* Components */
import { ChartComponent } from './chart/chart.component';
import { ChartModule } from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import * as highcharts from 'highcharts';
//import * as highmaps from 'highcharts/js/modules/map';

import { AchillesService } from './services/achilles.service';

import { DataBrowserHeaderComponent } from './data-browser-header/data-browser-header.component';
import { TreeService } from './services/tree.service';

import { LocalStorageModule } from 'angular-2-local-storage';


import { HomeAsideComponent } from './home/home-aside/home-aside.component';
import { HomeInfoComponent } from './home/home-info/home-info.component';
import { HomeComponent } from './home/home.component';
import { LazyTreeComponent } from './lazy-tree/lazy-tree.component';
import { MobileChartsComponent } from './mobile-charts/mobile-charts.component';
import { MyConceptsComponent } from './my-concepts/my-concepts.component';
import { OneConceptComponent } from './one-concept/one-concept.component';
import { PlaceholderComponent } from './placeholder/placeholder.component';
import { RecursiveTreeComponent } from './recursive-tree/recursive-tree.component';
import { SearchTableAdvancedComponent } from './search-table-advanced/search-table-advanced.component';
import { SearchComponent } from './search/search.component';
import { SurveyDrawerComponent } from './survey-drawer/survey-drawer.component';
import { TreeContainerComponent } from './tree-container/tree-container.component';




import {DataBrowserService} from 'generated';
import {ConceptDrawerComponent} from './concept-drawer/concept-drawer.component';

/*Add highmaps to highchart in factory for workaround  to build problems with angular2-highcharts */
export function highchartsFactory() {
   // highmaps(highcharts);
    return highcharts;
}


@NgModule({
  imports: [
      BrowserModule,
      FormsModule,
      ReactiveFormsModule,
      ChartModule,
      // HttpClientModule,
      HttpModule,
      BrowserAnimationsModule,
      ClarityModule,
      LocalStorageModule.withConfig({
          prefix: 'my-app',
          storageType: 'localStorage'
      })
  ],
  declarations: [
      ChartComponent,
      DataBrowserHeaderComponent,
      ConceptDrawerComponent,
      SearchComponent,
      MyConceptsComponent,
      RecursiveTreeComponent,
      LazyTreeComponent,
      SurveyDrawerComponent,
      TreeContainerComponent,
      SearchTableAdvancedComponent,
      HomeComponent,
      HomeAsideComponent,
      HomeInfoComponent,
      MobileChartsComponent,
      OneConceptComponent,
      PlaceholderComponent


  ],
  providers: [
      AchillesService,
      DataBrowserService,
      TreeService,
      {
        provide: HighchartsStatic, useFactory: highchartsFactory
      }
  ]
})
export class DataBrowserModule {
  constructor() {}
}
