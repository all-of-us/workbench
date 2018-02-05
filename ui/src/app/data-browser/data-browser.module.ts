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

import {AchillesService} from './services/achilles.service';

import {DataBrowserHeaderComponent} from './data-browser-header/data-browser-header.component';
import {TreeService} from './services/tree.service';

import {LocalStorageModule} from 'angular-2-local-storage';

import { overriddenPublicUrlKey } from '../views/app/component';
import { HomeAsideComponent } from './home/home-aside/home-aside.component';
import { HomeInfoComponent } from './home/home-info/home-info.component';
import { HomeComponent } from './home/home.component';
import { LazyTreeComponent } from './lazy-tree/lazy-tree.component';
import { MobileChartsComponent } from './mobile-charts/mobile-charts.component';
import { MyConceptsComponent } from './my-concepts/my-concepts.component';
import { OneConceptComponent } from './one-concept/one-concept.component';
import { PlaceholderComponent } from './placeholder/placeholder.component';
import { SearchTableComponent } from './search-table/search-table.component';
import { SearchComponent } from './search/search.component';
import { TreeContainerComponent } from './tree-container/tree-container.component';

import {DataBrowserService} from 'publicGenerated';
import {ConceptDrawerComponent} from './concept-drawer/concept-drawer.component';

function getPublicBasePath() {
  return localStorage.getItem(overriddenPublicUrlKey) || environment.publicApiUrl;
}

const DataBrowserServiceFactory = (http: Http) => {
  return new DataBrowserService(http, getPublicBasePath(), null);
};

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ChartModule.forRoot(highcharts),
    HttpModule,
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
    LazyTreeComponent,
    TreeContainerComponent,
    SearchTableComponent,
    HomeComponent,
    HomeAsideComponent,
    HomeInfoComponent,
    MobileChartsComponent,
    OneConceptComponent,
    PlaceholderComponent
  ],
  providers: [
      AchillesService,
      {
        provide: DataBrowserService,
        useFactory: DataBrowserServiceFactory,
        deps: [Http]
      },
      TreeService,
      {
        provide: HighchartsStatic,
        useValue: highcharts,
      }
  ]
})
export class DataBrowserModule {
  constructor() {}
}
