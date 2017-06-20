// Import all the pieces of the app centrally.

import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';
import {BrowserModule} from '@angular/platform-browser';

// Imports for loading & configuring the in-memory web api
import {InMemoryWebApiModule} from 'angular-in-memory-web-api';

import {AppRoutingModule} from './app-routing.module'
import {AppComponent} from './app.component';
import {CohortBuilderComponent} from './cohort-builder.component'
import {InMemoryDataService} from './in-memory-data.service';
import {LoginComponent} from './login.component'
import {RepositoryService} from './repository.service'
import {SelectRepositoryComponent} from './select-repository.component'
import {UserService} from './user.service'
import {VAADIN_CLIENT} from './vaadin-client';

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
    // If the Vaadin javascript file fails to load, the "vaadin" symbol doesn't get defined,
    // and referencing it directly results in an error.
    {provide: VAADIN_CLIENT, useValue: typeof vaadin === "undefined" ? undefined : vaadin}
  ],

  // This specifies the top-level component, to load first.
  bootstrap: [AppComponent]
})
export class AppModule {}
