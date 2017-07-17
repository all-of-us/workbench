// Import all the pieces of the app centrally.

import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from 'app/app-routing.module';
import {AppComponent} from 'app/views/app/component';
import {CohortBuilderComponent} from 'app/views/cohort-builder/component';
import {LoginComponent} from 'app/views/login/component';
import {RepositoryService} from 'app/services/repository.service';
import {SelectRepositoryComponent} from 'app/views/select-repository/component';
import {SignInService} from 'app/services/sign-in.service';
import {UserService} from 'app/services/user.service';
import {VAADIN_CLIENT} from 'app/vaadin-client';
import {WorkspaceComponent} from 'app/views/workspace/component'
import {CohortsService, Configuration, ConfigurationParameters} from 'generated';
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
    FormsModule,
    HttpModule
  ],
  declarations: [
    AppComponent,
    LoginComponent,
    SelectRepositoryComponent,
    CohortBuilderComponent,
    WorkspaceComponent
  ],
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
    CohortsService
  ],

  // This specifies the top-level component, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
