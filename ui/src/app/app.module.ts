// Import all the pieces of the app centrally.

import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from 'clarity-angular';

import {AppRoutingModule} from 'app/app-routing.module';
import {AppComponent} from 'app/views/app/component';
import {CohortBuilderComponent} from
    'app/views/cohort-builder/search/cohort-builder/cohort-builder.component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {LoginComponent} from 'app/views/login/component';
import {RepositoryService} from 'app/services/repository.service';
import {SelectRepositoryComponent} from 'app/views/select-repository/component';
import {SignInService} from 'app/services/sign-in.service';
import {UserService} from 'app/services/user.service';
import {VAADIN_CLIENT} from 'app/vaadin-client';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {CohortsService, WorkspacesService, Configuration, ConfigurationParameters} from 'generated';
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
import { BroadcastService, SearchService } from './views/cohort-builder/search/service';

export function getVaadin(): VaadinNs {
  // If the Vaadin javascript file fails to load, the "vaadin" symbol doesn't get defined,
  // and referencing it directly results in an error.
  if (typeof vaadin === 'undefined') {
    return undefined;
  } else {
    return vaadin;
  }
}

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
    LoginComponent,
    SelectRepositoryComponent,
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
    CohortEditComponent,
    HomePageComponent,
    WorkspaceComponent,
    WorkspaceEditComponent
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    UserService,
    RepositoryService,
    SignInService,
    {provide: VAADIN_CLIENT, useFactory: getVaadin},
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    CohortsService,
    WorkspacesService,
    BroadcastService,
    SearchService
  ],

  // This specifies the top-level component, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
