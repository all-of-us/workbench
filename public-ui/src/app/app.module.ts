import {ErrorHandler, NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http, HttpModule, RequestOptions, XHRBackend} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {environment} from 'environments/environment';
import * as StackTrace from 'stacktrace-js';

import {AppComponent, overriddenUrlKey} from './views/app/component';

import {RoutingSpinnerComponent} from './views/routing-spinner/component';


/* Our Modules */
import {
  ApiModule,
} from 'publicGenerated';


import {AppRoutingModule} from './app-routing.module';
import {DataBrowserModule} from './data-browser/data-browser.module';
import { DbHeaderComponent } from './views/db-header/db-header.component';
import { DbHomeComponent } from './views/db-home/db-home.component';
import { SurveyViewComponent } from './views/survey-view/survey-view.component';
import { SurveysComponent } from './views/surveys/surveys.component';

// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

import {DataBrowserService} from 'publicGenerated';
import { overriddenPublicUrlKey } from './views/app/component';
import { QuickSearchComponent } from './views/quick-search/quick-search.component';
import { EhrViewComponent } from './views/ehr-view/ehr-view.component';
import { HighlightSearchPipe } from './utils/highlight-search.pipe';
import { PhysicalMeasurementsComponent } from './views/physical-measurements/physical-measurements.component';

function getPublicBasePath() {
  return localStorage.getItem(overriddenPublicUrlKey) || environment.publicApiUrl;
}

const DataBrowserServiceFactory = (http: Http) => {
  return new DataBrowserService(http, getPublicBasePath(), null);
};

@NgModule({
  imports: [
    ApiModule,
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,
    NgxChartsModule,
    ClarityModule,
    DataBrowserModule
  ],
  declarations: [
    AppComponent,
    RoutingSpinnerComponent,
    SurveysComponent,
    DbHeaderComponent,
    SurveyViewComponent,
    DbHomeComponent,
    QuickSearchComponent,
    EhrViewComponent,
    HighlightSearchPipe,
    PhysicalMeasurementsComponent,
  ],
  providers: [
    {
      provide: DataBrowserService,
      useFactory: DataBrowserServiceFactory,
      deps: [Http]
    },
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent]

})
export class AppModule {}
