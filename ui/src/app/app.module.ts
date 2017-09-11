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

import {AppRoutingModule} from 'app/app-routing.module';
import {AppComponent} from 'app/views/app/component';
import {BugReportComponent} from 'app/views/bug-report/component';
import {CohortBuilderComponent} from 'app/views/cohort-builder/search/cohort-builder/cohort-builder.component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {SignInService} from 'app/services/sign-in.service';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {CohortsService, WorkspacesService, BugReportService, Configuration, ConfigurationParameters, CohortBuilderService} from 'generated';
import {environment} from 'environments/environment';
import { SearchGroupComponent } from 'app/views/cohort-builder/search/search-group/search-group.component';
import { CriteriaTreeComponent } from 'app/views/cohort-builder/search/criteria-tree/criteria-tree.component';
import { CriteriaGroupComponent } from 'app/views/cohort-builder/search/criteria-group/criteria-group.component';
import { SearchResultComponent } from 'app/views/cohort-builder/search/search-result/search-result.component';
import { GoogleChartDirective } from 'app/views/cohort-builder/search/google-chart/google-chart.directive';
import { GenderChartComponent } from 'app/views/cohort-builder/search/gender-chart/gender-chart.component';
import { RaceChartComponent } from 'app/views/cohort-builder/search/race-chart/race-chart.component';
import { CohortReviewComponent } from 'app/views/cohort-builder/review/cohort-review/cohort-review.component';
import { SubjectListComponent } from 'app/views/cohort-builder/review/subject-list/subject-list.component';
import { SubjectDetailComponent } from 'app/views/cohort-builder/review/subject-detail/subject-detail.component';
import { AnnotationsComponent } from 'app/views/cohort-builder/review/annotations/annotations.component';
import { MedicationsComponent } from 'app/views/cohort-builder/review/medications/medications.component';
import { WizardSelectComponent } from 'app/views/cohort-builder/search/wizard-select/wizard-select.component';
import { WizardModalComponent } from 'app/views/cohort-builder/search/wizard-modal/wizard-modal.component';
import { WizardTreeParentComponent } from './views/cohort-builder/search/wizard-tree-parent/wizard-tree-parent.component';
import { BroadcastService, SearchService } from './views/cohort-builder/search/service';

// tslint:enable:max-line-length


// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
    return new Configuration({
      basePath: environment.allOfUsApiUrl,
      accessToken: () => signInService.currentAccessToken
    });
}



@NgModule({
  imports:      [
    AppRoutingModule,
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    ClarityModule.forRoot()
  ],
  declarations: [
    AppComponent,
    BugReportComponent,
    CohortBuilderComponent,
    SearchGroupComponent,
    CriteriaTreeComponent,
    CriteriaGroupComponent,
    SearchResultComponent,
    GoogleChartDirective,
    GenderChartComponent,
    RaceChartComponent,
    CohortReviewComponent,
    SubjectListComponent,
    SubjectDetailComponent,
    AnnotationsComponent,
    MedicationsComponent,
    WizardSelectComponent,
    WizardModalComponent,
    WizardTreeParentComponent,
    CohortEditComponent,
    HomePageComponent,
    WorkspaceComponent,
    WorkspaceEditComponent
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    SignInService,
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    CohortsService,
    WorkspacesService,
    BroadcastService,
    ProfileService,
    SearchService,
    CohortBuilderService,
    BugReportService
  ],

  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent, BugReportComponent]
})
export class AppModule {}
