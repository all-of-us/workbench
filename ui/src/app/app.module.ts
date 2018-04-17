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
import {ServerConfigService} from './services/server-config.service';
import {SignInService} from './services/sign-in.service';
import {StatusCheckService} from './services/status-check.service';

import {AccountCreationSuccessComponent} from './views/account-creation-success/component';
import {AccountCreationComponent} from './views/account-creation/component';
import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {AppComponent, overriddenUrlKey} from './views/app/component';
import {BreadcrumbComponent} from './views/breadcrumb/component';
import {BugReportComponent} from './views/bug-report/component';
import {CohortEditComponent} from './views/cohort-edit/component';
import {ErrorHandlerComponent} from './views/error-handler/component';
import {InvitationKeyComponent} from './views/invitation-key/component';
import {LoginComponent} from './views/login/component';
import {NotFoundComponent} from './views/not-found/component';
import {PageTemplateSignedOutComponent} from './views/page-template-signed-out/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {RoutingSpinnerComponent} from './views/routing-spinner/component';
import {SignedInComponent} from './views/signed-in/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceListComponent} from './views/workspace-list/component';
import {WorkspaceNavBarComponent} from './views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {DataBrowserModule} from './data-browser/data-browser.module';
import {IconsModule} from './icons/icons.module';

import {
  ApiModule,
  ConfigService,
  Configuration,
  StatusService
} from 'generated';

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

@NgModule({
  imports: [
    ApiModule,
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
    AccountCreationComponent,
    AccountCreationSuccessComponent,
    AdminReviewWorkspaceComponent,
    AdminReviewIdVerificationComponent,
    AppComponent,
    BreadcrumbComponent,
    BugReportComponent,
    CohortEditComponent,
    ErrorHandlerComponent,
    WorkspaceListComponent,
    InvitationKeyComponent,
    LoginComponent,
    NotFoundComponent,
    PageTemplateSignedOutComponent,
    ProfilePageComponent,
    RoutingSpinnerComponent,
    SignedInComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceNavBarComponent,
    WorkspaceShareComponent,
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
    ErrorHandlingService,
    ServerConfigService,
    {
      provide: ErrorHandler,
      deps: [ServerConfigService],
      useClass: ErrorReporterService,
    },
    SignInService,
    StatusCheckService,
    GoogleAnalyticsEventsService,
    {
      provide: Http,
      useClass: InterceptedHttp,
      deps: [XHRBackend, RequestOptions, ErrorHandlingService]
    },
    NotFoundComponent,
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, ErrorHandlerComponent, NotFoundComponent]
})
export class AppModule {}
