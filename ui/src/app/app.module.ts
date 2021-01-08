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

import {AppRouting} from './app-routing';
import {BugReportComponent} from './components/bug-report';
import {ConfirmDeleteModalComponent} from './components/confirm-delete-modal';
import {HelpSidebarComponent} from './components/help-sidebar';
import {RoutingSpinnerComponent} from './components/routing-spinner/component';
import {AdminBannerComponent} from './pages/admin/admin-banner';
import {AdminReviewWorkspaceComponent} from './pages/admin/admin-review-workspace';
import {AdminUserComponent} from './pages/admin/admin-user';
import {AdminUsersComponent} from './pages/admin/admin-users';
import {AdminWorkspaceComponent} from './pages/admin/admin-workspace';
import {AppComponent, overriddenUrlKey} from './pages/app/component';
import {CohortReviewComponent} from './pages/data/cohort-review/cohort-review';
import {DetailPageComponent} from './pages/data/cohort-review/detail-page';
import {QueryReportComponent} from './pages/data/cohort-review/query-report.component';
import {TablePage} from './pages/data/cohort-review/table-page';
import {CohortActionsComponent} from './pages/data/cohort/cohort-actions';
import {ConceptHomepageComponent} from './pages/data/concept/concept-homepage';
import {ConceptSearchComponent} from './pages/data/concept/concept-search';
import {ConceptSetActionsComponent} from './pages/data/concept/concept-set-actions';
import {InitialErrorComponent} from './pages/initial-error/component';
import {ProfilePageComponent} from './pages/profile/profile-page';
import {SignedInComponent} from './pages/signed-in/component';
import {WorkspaceAboutComponent} from './pages/workspace/workspace-about';
import {WorkspaceEditComponent} from './pages/workspace/workspace-edit';
import {WorkspaceListComponent} from './pages/workspace/workspace-list';
import {WorkspaceNavBarComponent} from './pages/workspace/workspace-nav-bar';
import {WorkspaceShareComponent} from './pages/workspace/workspace-share';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {IconsModule} from './icons/icons.module';
import {FetchModule} from './services/fetch.module';

import {
  ApiModule,
  ConfigService,
  Configuration,
} from 'generated';
import {Configuration as FetchConfiguration} from 'generated/fetch';
import {
  Configuration as LeoConfiguration,
} from 'notebooks-generated/fetch';

import {TextModalComponent} from 'app/components/text-modal';
import {AdminWorkspaceSearchComponent} from 'app/pages/admin/admin-workspace-search';
import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';
import {FooterComponent} from './components/footer';
import {AdminInstitutionComponent} from './pages/admin/admin-institution';
import {AdminInstitutionEditComponent} from './pages/admin/admin-institution-edit';



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
    AppRoutingModule,

    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,

    FetchModule,
    IconsModule,
    ClarityModule,
  ],
  declarations: [
    AdminBannerComponent,
    AdminInstitutionComponent,
    AdminInstitutionEditComponent,
    AdminWorkspaceComponent,
    AdminWorkspaceSearchComponent,
    AdminReviewWorkspaceComponent,
    AdminUserComponent,
    AdminUsersComponent,
    AppComponent,
    AppRouting,
    BugReportComponent,
    CohortActionsComponent,
    CohortReviewComponent,
    ConceptSetActionsComponent,
    ConceptHomepageComponent,
    ConceptSearchComponent,
    ConfirmDeleteModalComponent,
    DataPageComponent,
    DataSetPageComponent,
    DetailPageComponent,
    FooterComponent,
    HelpSidebarComponent,
    InitialErrorComponent,
    ProfilePageComponent,
    QueryReportComponent,
    RoutingSpinnerComponent,
    SignedInComponent,
    NavBarComponent,
    TablePage,
    TextModalComponent,
    WorkspaceAboutComponent,
    WorkspaceEditComponent,
    WorkspaceListComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
    WorkspaceWrapperComponent,
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
