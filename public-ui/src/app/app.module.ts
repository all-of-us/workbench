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
import {AppRoutingModule} from './app-routing.module';
import {DataBrowserModule} from './data-browser/data-browser.module';
import {IconsModule} from './icons/icons.module';
import { SurveysComponent } from './views/surveys/surveys.component';
import { DbHeaderComponent } from './views/db-header/db-header.component';



// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

function getBasePath() {
  return localStorage.getItem(overriddenUrlKey) || environment.allOfUsApiUrl;
}


@NgModule({
  imports: [
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,
    IconsModule,
    NgxChartsModule,
    ClarityModule,
    DataBrowserModule,
  ],
  declarations: [
    AppComponent,
    RoutingSpinnerComponent,
    SurveysComponent,
    DbHeaderComponent,
  ],
  providers: [
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
