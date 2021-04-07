import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouteReuseStrategy} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {WorkspaceWrapperComponent} from 'app/pages/workspace/workspace-wrapper/component';
import * as StackTrace from 'stacktrace-js';

import {CanDeactivateGuard} from './guards/can-deactivate-guard.service';
import {ProfileStorageService} from './services/profile-storage.service';
import {ServerConfigService} from './services/server-config.service';
import {SignInService} from './services/sign-in.service';
import {WINDOW_REF} from './utils';
import {WorkbenchRouteReuseStrategy} from './utils/navigation';

import {AppRouting} from './app-routing';
import {CohortPageComponent} from './cohort-search/cohort-page/cohort-page.component';
import {BugReportComponent} from './components/bug-report';
import {ConfirmDeleteModalComponent} from './components/confirm-delete-modal';
import {HelpSidebarComponent} from './components/help-sidebar';
import {RoutingSpinnerComponent} from './components/routing-spinner/component';
import {AppComponent} from './pages/app/component';
import {ConceptSearchComponent} from './pages/data/concept/concept-search';
import {InitialErrorComponent} from './pages/initial-error/component';
import {SignedInComponent} from './pages/signed-in/component';
import {WorkspaceNavBarComponent} from './pages/workspace/workspace-nav-bar';
import {WorkspaceShareComponent} from './pages/workspace/workspace-share';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {FetchModule} from './services/fetch.module';

import {TextModalComponent} from 'app/components/text-modal';
import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';
import {FooterComponent} from './components/footer';



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
    HttpModule,
    ReactiveFormsModule,

    FetchModule,
    ClarityModule,
  ],
  declarations: [
    AppComponent,
    AppRouting,
    BugReportComponent,
    CohortPageComponent,
    ConceptSearchComponent,
    ConfirmDeleteModalComponent,
    DataPageComponent,
    DataSetPageComponent,
    FooterComponent,
    HelpSidebarComponent,
    InitialErrorComponent,
    RoutingSpinnerComponent,
    SignedInComponent,
    NavBarComponent,
    TextModalComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
    WorkspaceWrapperComponent,
  ],
  providers: [
    ServerConfigService,
    ProfileStorageService,
    SignInService,
    {
      provide: WINDOW_REF,
      useValue: window
    },
    WorkbenchRouteReuseStrategy,
    {provide: RouteReuseStrategy, useExisting: WorkbenchRouteReuseStrategy},
    CanDeactivateGuard
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, InitialErrorComponent]
})
export class AppModule {}
