import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {
  ActivatedRoute,
  Event as RouterEvent,
  NavigationEnd,
  Router,
} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {SignInDetails, SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {Authority, ProfileService} from 'generated';

declare const gapi: any;
export const overriddenUrlKey = 'allOfUsApiUrlOverride';
export const overriddenPublicUrlKey = 'publicApiUrlOverride';


@Component({
  selector: 'app-signed-out',
  styleUrls: ['./component.css',
              '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class SignedOutAppComponent implements OnInit {
  user: Observable<SignInDetails>;
  hasReviewResearchPurpose = false;
  hasReviewIdVerification = false;
  currentUrl: string;
  email: string;
  backgroundImgSrc = '/assets/images/login-group.png';
  smallerBackgroundImgSrc = '/assets/images/login-standing.png';
  headerImg = '/assets/images/all-of-us-logo.svg';
  headerHeight = 102;
  sidenavToggle = false;
  isSignedIn = false;
  private baseTitle: string;
  private overriddenUrl: string = null;
  private showCreateAccount = false;
  private overriddenPublicUrl: string = null;

  constructor(
    /* Ours */
    private signInService: SignInService,
    private profileService: ProfileService,
    /* Angular's */
    private activatedRoute: ActivatedRoute,
    private locationService: Location,
    private router: Router,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    this.currentUrl = this.router.url;
  }

  signIn(e: Event): void {
    this.signInService.signIn();
  }

  getTopMargin(): string {
    return this.showCreateAccount ? '10vh' : '30vh';
  }

  get reviewWorkspaceActive(): boolean {
    return this.locationService.path().startsWith('/admin/review-workspace');
  }

  get reviewIdActive(): boolean {
    return this.locationService.path().startsWith('/admin/review-id-verification');
  }

  get workspacesActive(): boolean {
    return this.locationService.path() === ''
      || this.locationService.path().startsWith('/workspace');
  }

}
