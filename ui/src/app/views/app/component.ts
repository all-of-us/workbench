// UI Component framing the overall app (title and nav).
// Content is in other Components.

import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {SignInDetails, SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {CohortsService, Configuration, ConfigurationParameters, ProfileService} from 'generated';

declare const gapi: any;

@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  private baseTitle: string;
  user: Observable<SignInDetails>;
  hasAdminPermissions = false;

  constructor(
      private activatedRoute: ActivatedRoute,
      private cohortsService: CohortsService,
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private router: Router,
      private signInService: SignInService,
      private titleService: Title
  ) {}

  ngOnInit(): void {
    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.

    this.baseTitle = this.titleService.getTitle();
    if (environment.displayTag) {
      this.baseTitle = `[${environment.displayTag}] ${this.baseTitle}`;
      this.titleService.setTitle(this.baseTitle);
    }

    // After navigation events, get the "title" value of the current Route and
    // include it in the web page title.
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        let currentRoute = this.activatedRoute;
        while (currentRoute.firstChild) {
          currentRoute = currentRoute.firstChild;
        }
        if (currentRoute.outlet === 'primary') {
          currentRoute.data.subscribe(value =>
              this.titleService.setTitle(`${value.title} | ${this.baseTitle}`));
        }
      }
    });
    this.user = this.signInService.user;
    this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(profile => {
      // TODO(RW-85) Real UI for research purpose review. This is a standin to demonstrate that
      // we can fetch permissions from the frontend code.
      this.hasAdminPermissions = profile.authorities.length > 0;
    });
  }

  signIn(e: Event): void {
    this.signInService.signIn();
  }

  signOut(e: Event): void {
    this.signInService.signOut();
  }
}
