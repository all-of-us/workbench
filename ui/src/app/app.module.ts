import {HttpClientModule} from '@angular/common/http';
import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouteReuseStrategy} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import * as StackTrace from 'stacktrace-js';

import {NavigationGuard} from 'app/guards/navigation-guard';
import {SignInService} from './services/sign-in.service';
import {WINDOW_REF} from './utils';
import {WorkbenchRouteReuseStrategy} from './utils/navigation';

import {AppRouting} from './app-routing';
import {HelpSidebarComponent} from './components/help-sidebar';
import {AppComponent} from './pages/app/component';
import {InitialErrorComponent} from './pages/initial-error/component';
import {WorkspaceNavBarComponent} from './pages/workspace/workspace-nav-bar';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {FetchModule} from './services/fetch.module';

import {FooterComponent} from 'app/components/footer';
import {TextModalComponent} from 'app/components/text-modal';
import {ZendeskWidgetComponent} from 'app/components/zendesk-widget';
import {InactivityMonitorComponent} from 'app/pages/signed-in/inactivity-monitor';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';


// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

@NgModule({
  imports: [
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    ReactiveFormsModule,
    FetchModule,
    ClarityModule,
  ],
  declarations: [
    AppComponent,
    AppRouting,
    FooterComponent,
    HelpSidebarComponent,
    InactivityMonitorComponent,
    InitialErrorComponent,
    NavBarComponent,
    TextModalComponent,
    WorkspaceNavBarComponent,
    ZendeskWidgetComponent
  ],
  providers: [
    SignInService,
    {
      provide: WINDOW_REF,
      useValue: window
    },
    WorkbenchRouteReuseStrategy,
    {provide: RouteReuseStrategy, useExisting: WorkbenchRouteReuseStrategy},
    NavigationGuard
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, InitialErrorComponent]
})
export class AppModule {}
