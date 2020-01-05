import {ErrorHandler, NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http, HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouteReuseStrategy} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {WorkspaceWrapperComponent} from 'app/pages/workspace/workspace-wrapper/component';
import {environment} from 'environments/environment';
import * as StackTrace from 'stacktrace-js';

import {CanDeactivateGuard} from './guards/can-deactivate-guard.service';
import {CdrVersionStorageService} from './services/cdr-version-storage.service';
import {ErrorReporterService} from './services/error-reporter.service';
import {GoogleAnalyticsEventsService} from './services/google-analytics-events.service';
import {ProfileStorageService} from './services/profile-storage.service';
import {ServerConfigService} from './services/server-config.service';
import {SignInService} from './services/sign-in.service';
import {cookiesEnabled, WINDOW_REF} from './utils';
import {WorkbenchRouteReuseStrategy} from './utils/navigation';

import {BugReportComponent} from './components/bug-report';
import {HelpSidebarComponent} from './components/help-sidebar';
import {RoutingSpinnerComponent} from './components/routing-spinner/component';
import {AdminBannerComponent} from './pages/admin/admin-banner';
import {AdminReviewWorkspaceComponent} from './pages/admin/admin-review-workspace';
import {AdminUserComponent} from './pages/admin/admin-user';
import {NotebookListComponent} from './pages/analysis/notebook-list';
import {NotebookRedirectComponent} from './pages/analysis/notebook-redirect';
import {AppComponent, overriddenUrlKey} from './pages/app/component';
import {CohortReviewComponent} from './pages/data/cohort-review/cohort-review';
import {CreateReviewModalComponent} from './pages/data/cohort-review/create-review-modal';
import {DetailPageComponent} from './pages/data/cohort-review/detail-page';
import {QueryReportComponent} from './pages/data/cohort-review/query-report.component';
import {TablePage} from './pages/data/cohort-review/table-page';
import {CohortActionsComponent} from './pages/data/cohort/cohort-actions';
import {ConceptHomepageComponent} from './pages/data/concept/concept-homepage';
import {ConceptSetActionsComponent} from './pages/data/concept/concept-set-actions';
import {ConceptSetDetailsComponent} from './pages/data/concept/concept-set-details';
import {HomepageComponent} from './pages/homepage/homepage';
import {InitialErrorComponent} from './pages/initial-error/component';
import {PageTemplateSignedOutComponent} from './pages/login/page-template-signed-out/component';
import {SignInComponent} from './pages/login/sign-in';
import {DataUseAgreementComponent} from './pages/profile/data-use-agreement';
import {ProfilePageComponent} from './pages/profile/profile-page';
import {SignedInComponent} from './pages/signed-in/component';
import {StigmatizationPageComponent} from './pages/workspace/stigmatization-page';
import {WorkspaceAboutComponent} from './pages/workspace/workspace-about';
import {WorkspaceEditComponent} from './pages/workspace/workspace-edit';
import {WorkspaceLibraryComponent} from './pages/workspace/workspace-library';
import {WorkspaceListComponent} from './pages/workspace/workspace-list';
import {WorkspaceNavBarComponent} from './pages/workspace/workspace-nav-bar';
import {WorkspaceShareComponent} from './pages/workspace/workspace-share';

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
import {
  ApiModule as LeoApiModule,
  Configuration as LeoConfiguration,
} from 'notebooks-generated';

import {TextModalComponent} from 'app/components/text-modal';
import {InteractiveNotebookComponent} from 'app/pages/analysis/interactive-notebook';
import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';



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
    AdminBannerComponent,
    AdminReviewWorkspaceComponent,
    AdminUserComponent,
    AppComponent,
    BugReportComponent,
    CohortActionsComponent,
    CohortReviewComponent,
    ConceptSetActionsComponent,
    ConceptSetDetailsComponent,
    ConceptHomepageComponent,
    CreateReviewModalComponent,
    DataPageComponent,
    DataSetPageComponent,
    DataUseAgreementComponent,
    DetailPageComponent,
    HelpSidebarComponent,
    InitialErrorComponent,
    InteractiveNotebookComponent,
    NotebookListComponent,
    NotebookRedirectComponent,
    PageTemplateSignedOutComponent,
    ProfilePageComponent,
    QueryReportComponent,
    RoutingSpinnerComponent,
    SignedInComponent,
    NavBarComponent,
    SignInComponent,
    StigmatizationPageComponent,
    TablePage,
    TextModalComponent,
    WorkspaceAboutComponent,
    WorkspaceEditComponent,
    WorkspaceLibraryComponent,
    WorkspaceListComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
    WorkspaceWrapperComponent,
    HomepageComponent,
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
    ServerConfigService,
    {
      provide: ErrorHandler,
      deps: [ServerConfigService],
      useClass: ErrorReporterService,
    },
    CdrVersionStorageService,
    ProfileStorageService,
    SignInService,
    GoogleAnalyticsEventsService,
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
