// UI Component framing the overall app (title and nav).
// Content is in other Components.

import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInDetails, SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {Authority, ProfileService} from 'generated';

/* tslint:disable-next-line:no-unused-variable */
declare const gapi: any;

@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  private baseTitle: string;
  user: Observable<SignInDetails>;
  hasReviewResearchPurpose = false;
  private _showCreateAccount = false;

  constructor(
      private activatedRoute: ActivatedRoute,
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
    this.user.subscribe(user => {
      if (user.isSignedIn) {
        this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(profile => {
          this.hasReviewResearchPurpose =
            profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        });
      }
    });
  }

  signIn(e: Event): void {
    this.signInService.signIn();
  }

  signOut(e: Event): void {
    this.signInService.signOut();
  }

  showCreateAccount(): void {
    this._showCreateAccount = true;
  }

  getTopMargin(): string {
    return this._showCreateAccount ? '10vh' : '30vh';
  }

  deleteAccount(): void {
    this.profileService.deleteAccount().subscribe(() => {
      this.signInService.signOut();
    });
  }
}
