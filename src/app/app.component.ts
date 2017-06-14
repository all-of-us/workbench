// UI Component framing the overall app (title and nav).
// Content is in other Components.

import {Component} from '@angular/core';

@Component({
  selector: 'aou-app',
  styleUrls: ['./app.component.css'],
  template: `
    <div>
      <img class="main-logo" src="images/all-of-us-logo.svg" alt="All of Us"
        style="width: 18rem;">
      <img class="portal-logo" src="images/portal-logo.svg" alt="Researcher Portal"
        style="margin-left: 0.5rem; width: 13rem;">
    </div>
    <nav style="margin-top: 1rem;">
      <a routerLink="/login" routerLinkActive="active">Switch User</a>
      <a routerLink="/repository" routerLinkActive="active">Select CDR</a>
    </nav>
    <!--
      Angular's builtin Router module will attach specific views here
      based on clicking routerLink elements (above) or router.navigate calls.
    -->
    <router-outlet></router-outlet>
  `
})

export class AppComponent {}
