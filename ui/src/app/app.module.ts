import {ErrorHandler, NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http, HttpModule, RequestOptions, XHRBackend} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouteReuseStrategy} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';

import {environment} from 'environments/environment';
import * as StackTrace from 'stacktrace-js';

import {InterceptedHttp} from './factory/InterceptedHttp';
import {CdrVersionStorageService} from './services/cdr-version-storage.service';
import {ErrorHandlingService} from './services/error-handling.service';
import {ErrorReporterService} from './services/error-reporter.service';
import {GoogleAnalyticsEventsService} from './services/google-analytics-events.service';
import {ProfileStorageService} from './services/profile-storage.service';
import {ServerConfigService} from './services/server-config.service';
import {SignInService} from './services/sign-in.service';
import {StatusCheckService} from './services/status-check.service';
import {cookiesEnabled, WINDOW_REF} from './utils';
import {WorkbenchRouteReuseStrategy} from './utils/navigation';

import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {AdminUserBypassComponent} from './views/admin-user-bypass';
import {AdminUserComponent} from './views/admin-user/component';
import {AppComponent, overriddenUrlKey} from './views/app/component';
import {BreadcrumbComponent} from './views/breadcrumb';
import {BugReportComponent} from './views/bug-report';
import {CohortActionsComponent} from './views/cohort-actions';
import {CohortListComponent} from './views/cohort-list';
import {ConceptAddModalComponent} from './views/concept-add-modal';
import {ConceptHomepageComponent} from './views/concept-homepage';
import {ConceptSetActionsComponent} from './views/concept-set-actions';
import {ConceptSetDetailsComponent} from './views/concept-set-details';
import {ConceptSetListComponent} from './views/concept-set-list';
import {ConceptTableComponent} from './views/concept-table';
import {DataUseAgreementComponent} from './views/data-use-agreement';
import {ErrorHandlerComponent} from './views/error-handler/component';
import {HomepageComponent} from './views/homepage';
import {InitialErrorComponent} from './views/initial-error/component';
import {NewNotebookModalComponent} from './views/new-notebook-modal';
import {NotebookListComponent} from './views/notebook-list';
import {NotebookRedirectComponent} from './views/notebook-redirect/component';
import {PageTemplateSignedOutComponent} from './views/page-template-signed-out/component';
import {ProfilePageComponent} from './views/profile-page';
import {QuickTourModalComponent} from './views/quick-tour-modal';
import {RecentWorkComponent} from './views/recent-work';
import {ResetClusterButtonComponent} from './views/reset-cluster-button';
import {RoutingSpinnerComponent} from './views/routing-spinner/component';
import {SignInComponent} from './views/sign-in';
import {SignedInComponent} from './views/signed-in/component';
import {SlidingFabComponent} from './views/sliding-fab';
import {StigmatizationPageComponent} from './views/stigmatization-page';
import {TopBoxComponent} from './views/top-box/component';
import {WorkspaceAboutComponent} from './views/workspace-about';
import {WorkspaceEditComponent} from './views/workspace-edit';
import {WorkspaceLibraryComponent} from './views/workspace-library';
import {WorkspaceListComponent} from './views/workspace-list';
import {WorkspaceNavBarComponent} from './views/workspace-nav-bar';
import {WorkspaceShareComponent} from './views/workspace-share';
import {WorkspaceComponent} from './views/workspace/component';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {CohortCommonModule} from './cohort-common/module';
import {IconsModule} from './icons/icons.module';
import {FetchModule} from './services/fetch.module';

import {
  ApiModule,
  ConfigService,
  Configuration,
} from 'generated';

import {Configuration as FetchConfiguration} from 'generated/fetch';

import {DataPageComponent} from 'app/views/data-page';
import {DataSetPageComponent} from 'app/views/dataset-page';
import {
  ApiModule as LeoApiModule,
  Configuration as LeoConfiguration,
} from 'notebooks-generated';
import {InteractiveNotebookComponent} from './views/interactive-notebook';
import {ToolTipComponent} from './views/tooltip/component';


// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

function getBasePath() {
  if (cookiesEnabled()) {
    return localStorage.getItem(overriddenUrlKey) || environment.allOfUsApiUrl;
  } else {
    return environment.allOfUsApiUrl;
  }
}

export function getConfigService(http: Http) {
  return new ConfigService(http, getBasePath(), null);
}

// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
  return new Configuration({
    basePath: getBasePath(),
    accessToken: () => signInService.currentAccessToken
  });
}

export function getLeoConfiguration(signInService: SignInService): LeoConfiguration {
  return new LeoConfiguration({
    basePath: environment.leoApiUrl,
    accessToken: () => signInService.currentAccessToken
  });
}

@NgModule({
  imports: [
    ApiModule,
    LeoApiModule,
    AppRoutingModule,

    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,

    CohortCommonModule,
    FetchModule,
    IconsModule,
    ClarityModule,
  ],
  declarations: [
    AdminReviewWorkspaceComponent,
    AdminUserBypassComponent,
    AdminUserComponent,
    AppComponent,
    BreadcrumbComponent,
    BugReportComponent,
    CohortActionsComponent,
    CohortListComponent,
    ConceptAddModalComponent,
    ConceptSetActionsComponent,
    ConceptSetDetailsComponent,
    ConceptHomepageComponent,
    ConceptTableComponent,
    ConceptSetListComponent,
    DataPageComponent,
    DataSetPageComponent,
    DataUseAgreementComponent,
    ErrorHandlerComponent,
    InitialErrorComponent,
    NewNotebookModalComponent,
    InteractiveNotebookComponent,
    NotebookListComponent,
    NotebookRedirectComponent,
    PageTemplateSignedOutComponent,
    ProfilePageComponent,
    QuickTourModalComponent,
    RecentWorkComponent,
    RoutingSpinnerComponent,
    SignedInComponent,
    SignInComponent,
    SlidingFabComponent,
    StigmatizationPageComponent,
    ToolTipComponent,
    TopBoxComponent,
    WorkspaceAboutComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceLibraryComponent,
    WorkspaceListComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
    WorkspaceWrapperComponent,
    HomepageComponent,
    ResetClusterButtonComponent,
  ],
  providers: [
    {
      provide: ConfigService,
      deps: [Http],
      useFactory: getConfigService
    },
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    {
      provide: LeoConfiguration,
      deps: [SignInService],
      useFactory: getLeoConfiguration
    },
    {
      provide: FetchConfiguration,
      deps: [Configuration],
      useFactory: (c: Configuration) => new FetchConfiguration({
        accessToken: c.accessToken,
        basePath: c.basePath
      })
    },
    ErrorHandlingService,
    ServerConfigService,
    {
      provide: ErrorHandler,
      deps: [ServerConfigService],
      useClass: ErrorReporterService,
    },
    CdrVersionStorageService,
    ProfileStorageService,
    SignInService,
    StatusCheckService,
    GoogleAnalyticsEventsService,
    {
      provide: Http,
      useClass: InterceptedHttp,
      deps: [XHRBackend, RequestOptions, ErrorHandlingService]
    },
    {
      provide: WINDOW_REF,
      useValue: window
    },
    WorkbenchRouteReuseStrategy,
    {provide: RouteReuseStrategy, useExisting: WorkbenchRouteReuseStrategy}
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, ErrorHandlerComponent, InitialErrorComponent]
})
export class AppModule {}
