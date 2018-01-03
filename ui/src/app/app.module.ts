import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Http, HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from './services/error-handling.service';
import {SignInService} from './services/sign-in.service';

import {AccountCreationComponent} from './views/account-creation/component';
import {AppComponent, overriddenUrlKey} from './views/app/component';
import {BugReportComponent} from './views/bug-report/component';
import {CohortEditComponent} from './views/cohort-edit/component';
import {ErrorHandlerComponent} from './views/error-handler/component';
import {HomePageComponent} from './views/home-page/component';
import {IdVerificationPageComponent} from './views/id-verification-page/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {ReviewComponent} from './views/review/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

import {environment} from 'environments/environment';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {CohortReviewModule} from './cohort-review/cohort-review.module';
import {CohortSearchModule} from './cohort-search/cohort-search.module';
import {DataBrowserModule} from './data-browser/data-browser.module';

import {
  BugReportService,
  ClusterService,
  CohortsService,
  ConfigService,
  Configuration,
  ProfileService,
  WorkspacesService
} from 'generated';

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
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ClarityModule.forRoot(),
    CohortSearchModule,
    CohortReviewModule,
    DataBrowserModule

  ],
  declarations: [
    AppComponent,
    AccountCreationComponent,
    BugReportComponent,
    CohortEditComponent,
    ErrorHandlerComponent,
    HomePageComponent,
    IdVerificationPageComponent,
    ProfilePageComponent,
    ReviewComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceShareComponent
  ],
  providers: [
    BugReportService,
    ClusterService,
    CohortsService,
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
    ProfileService,
    SignInService,
    WorkspacesService,
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, BugReportComponent, ErrorHandlerComponent]
})
export class AppModule {}
