// Import all the pieces of the app centrally.

import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from 'clarity-angular';

import {AppRoutingModule} from 'app/app-routing.module';
import {AppComponent} from 'app/views/app/component';
import {CohortBuilderComponent} from 'app/views/cohort-builder/component';
import {CohortBuilderPlaceholderComponent} from 'app/views/cohort-builder-placeholder/component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {SignInService} from 'app/services/sign-in.service';
import {VAADIN_CLIENT} from 'app/vaadin-client';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {CohortsService, WorkspacesService, Configuration, ConfigurationParameters} from 'generated';
import {environment} from 'environments/environment';

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
    CohortBuilderComponent,
    CohortBuilderPlaceholderComponent,
    CohortEditComponent,
    HomePageComponent,
    WorkspaceComponent,
    WorkspaceEditComponent
  ],
  providers: [
    SignInService,
    {provide: VAADIN_CLIENT, useFactory: getVaadin},
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    CohortsService,
    WorkspacesService
  ],

  // This specifies the top-level component, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
