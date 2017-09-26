// Import all the pieces of the app centrally.

// TODO: Remove the lint-disable comment once we can selectively ignore import lines.
// https://github.com/palantir/tslint/pull/3099
// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from 'clarity-angular';

/* Our Components */
import {AppComponent} from 'app/views/app/component';
import {BugReportComponent} from 'app/views/bug-report/component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {SignInService} from 'app/services/sign-in.service';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {environment} from 'environments/environment';

/* Our Modules */
import {AppRoutingModule} from 'app/app-routing.module';
import {CohortSearchModule} from './cohort-search/cohort-search.module';
import {CohortReviewModule} from './cohort-review/cohort-review.module';

import {BugReportService, CohortsService, Configuration, ConfigurationParameters, ProfileService, WorkspacesService} from 'generated';
// tslint:enable:max-line-length


// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
    return new Configuration({
      basePath: environment.allOfUsApiUrl,
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
    BugReportComponent,
    CohortEditComponent,
    HomePageComponent,
    WorkspaceComponent,
    WorkspaceEditComponent
  ],
  providers: [
    SignInService,
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    CohortsService,
    WorkspacesService,
    BugReportService,
    ProfileService
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, BugReportComponent]
})
export class AppModule {}
