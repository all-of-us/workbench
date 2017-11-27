import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
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
import {ReviewComponent} from './views/review/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

import {environment} from 'environments/environment';

/* Our Modules */
import {AppRoutingModule} from './app-routing.module';
import {CohortReviewModule} from './cohort-review/cohort-review.module';
import {CohortSearchModule} from './cohort-search/cohort-search.module';

import {
  BugReportService,
  ClusterService,
  CohortsService,
  Configuration,
  ProfileService,
  WorkspacesService
} from 'generated';

// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
    const basePath = localStorage.getItem(overriddenUrlKey) || environment.allOfUsApiUrl;
    return new Configuration({
      basePath: basePath,
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
    CohortReviewModule
  ],
  declarations: [
    AppComponent,
    AccountCreationComponent,
    BugReportComponent,
    CohortEditComponent,
    ErrorHandlerComponent,
    HomePageComponent,
    ReviewComponent,
    WorkspaceComponent,
    WorkspaceEditComponent,
    WorkspaceShareComponent
  ],
  providers: [
    BugReportService,
    ClusterService,
    CohortsService,
    ErrorHandlingService,
    ProfileService,
    SignInService,
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    WorkspacesService,
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, BugReportComponent, ErrorHandlerComponent]
})
export class AppModule {}
