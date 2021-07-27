import {HttpClientModule} from '@angular/common/http';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import * as StackTrace from 'stacktrace-js';

import {WINDOW_REF} from './utils';

import {AppComponent} from './pages/app/component';
import {InitialErrorComponent} from './pages/initial-error/component';


// TODO angular2react - we might need this?
// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    ReactiveFormsModule,
    ClarityModule,
  ],
  declarations: [
    AppComponent,
    InitialErrorComponent
  ],
  providers: [
    {
      provide: WINDOW_REF,
      useValue: window
    },
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, InitialErrorComponent]
})
export class AppModule {}
