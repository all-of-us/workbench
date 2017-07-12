// Import all the pieces of the app centrally.

import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';

// Imports for loading & configuring the in-memory web api
import {InMemoryWebApiModule} from 'angular-in-memory-web-api';

import {AppRoutingModule} from 'app/app-routing.module';
import {AppComponent} from 'app/views/app/component';
import {CohortBuilderComponent} from 'app/views/cohort-builder/component';
import {InMemoryDataService} from 'app/services/in-memory-data.service';
import {LoginComponent} from 'app/views/login/component';
import {RepositoryService} from 'app/services/repository.service';
import {SelectRepositoryComponent} from 'app/views/select-repository/component';
import {UserService} from 'app/services/user.service';
import {VAADIN_CLIENT} from 'app/vaadin-client';

export function getVaadin(): VaadinNs {
  // If the Vaadin javascript file fails to load, the "vaadin" symbol doesn't get defined,
  // and referencing it directly results in an error.
  if (typeof vaadin === 'undefined') {
    return undefined;
  } else {
    return vaadin;
  }
}

@NgModule({
  imports:      [
    AppRoutingModule,
    BrowserModule,
    FormsModule,
    HttpModule,
    InMemoryWebApiModule.forRoot(InMemoryDataService)
  ],
  declarations: [
    AppComponent,
    LoginComponent,
    SelectRepositoryComponent,
    CohortBuilderComponent
  ],
  providers: [
    UserService,
    RepositoryService,
    {provide: VAADIN_CLIENT, useFactory: getVaadin}
  ],

  // This specifies the top-level component, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
