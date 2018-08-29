import {ErrorHandler, NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http, HttpModule, RequestOptions, XHRBackend} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {environment} from 'environments/environment';
import * as StackTrace from 'stacktrace-js';

import {InterceptedHttp} from './factory/InterceptedHttp';
import {ErrorHandlingService} from './services/error-handling.service';
import {ErrorReporterService} from './services/error-reporter.service';
import {GoogleAnalyticsEventsService} from './services/google-analytics-events.service';
import {ProfileStorageService} from './services/profile-storage.service';
import {ServerConfigService} from './services/server-config.service';
import {SignInService} from './services/sign-in.service';
import {StatusCheckService} from './services/status-check.service';
import {WorkspaceStorageService} from './services/workspace-storage.service';
import {WINDOW_REF} from './utils';

import {AccountCreationModalsComponent} from './views/account-creation-modals/component';
import {AccountCreationSuccessComponent} from './views/account-creation-success/component';
import {AccountCreationComponent} from './views/account-creation/component';
import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {AppComponent, overriddenUrlKey} from './views/app/component';
import {BreadcrumbComponent} from './views/breadcrumb/component';
import {BugReportComponent} from './views/bug-report/component';
import {CohortEditModalComponent} from './views/cohort-edit-modal/component';
import {CohortListComponent} from './views/cohort-list/component';
import {ConfirmDeleteModalComponent} from './views/confirm-delete-modal/component';
import {ErrorHandlerComponent} from './views/error-handler/component';
import {HomepageComponent} from './views/homepage/component';
import {InitialErrorComponent} from './views/initial-error/component';
import {InvitationKeyComponent} from './views/invitation-key/component';
import {LoginComponent} from './views/login/component';
import {NotebookListComponent} from './views/notebook-list/component';
import {NotebookRedirectComponent} from './views/notebook-redirect/component';
import {PageTemplateSignedOutComponent} from './views/page-template-signed-out/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {RecentWorkComponent} from "./views/recent-work/component";
import {RenameModalComponent} from './views/rename-modal/component';
import {RoutingSpinnerComponent} from './views/routing-spinner/component';
import {SettingsComponent} from './views/settings/component';
import {SignedInComponent} from './views/signed-in/component';
import {StigmatizationPageComponent} from './views/stigmatization-page/component';
import {UnregisteredComponent} from './views/unregistered/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceListComponent} from './views/workspace-list/component';
import {WorkspaceNavBarComponent} from './views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {CohortCommonModule} from './cohort-common/module';
import {IconsModule} from './icons/icons.module';

import {
  ApiModule,
  ConfigService,
  Configuration,
} from 'generated';

import {
  ApiModule as LeoApiModule,
  Configuration as LeoConfiguration,
} from 'notebooks-generated';

// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

function getBasePath() {
  return localStorage.getItem(overriddenUrlKey) || environment.allOfUsApiUrl;
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
    IconsModule,
    NgxChartsModule,
    ClarityModule,
  ],
  declarations: [
    AccountCreationComponent,
    AccountCreationModalsComponent,
    AccountCreationSuccessComponent,
    AdminReviewWorkspaceComponent,
    AdminReviewIdVerificationComponent,
    AppComponent,
    BreadcrumbComponent,
    BugReportComponent,
    CohortEditModalComponent,
    CohortListComponent,
    ConfirmDeleteModalComponent,
    ErrorHandlerComponent,
    WorkspaceListComponent,
    InitialErrorComponent,
    InvitationKeyComponent,
    LoginComponent,
    NotebookListComponent,
    NotebookRedirectComponent,
    PageTemplateSignedOutComponent,
    ProfilePageComponent,
    RecentWorkComponent,
    RenameModalComponent,
    RoutingSpinnerComponent,
    SettingsComponent,
    SignedInComponent,
    StigmatizationPageComponent,
    UnregisteredComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
    HomepageComponent
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
    ErrorHandlingService,
    ServerConfigService,
    {
      provide: ErrorHandler,
      deps: [ServerConfigService],
      useClass: ErrorReporterService,
    },
    ProfileStorageService,
    SignInService,
    StatusCheckService,
    GoogleAnalyticsEventsService,
    WorkspaceStorageService,
    {
      provide: Http,
      useClass: InterceptedHttp,
      deps: [XHRBackend, RequestOptions, ErrorHandlingService]
    },
    {
      provide: WINDOW_REF,
      useValue: window
    }
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, ErrorHandlerComponent, InitialErrorComponent]
})
export class AppModule {}
