import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {
  ActivatedRoute,
  Event as RouterEvent,
} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {SignInDetails, SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {Authority, ProfileService} from 'generated';

declare const gapi: any;
export const overriddenUrlKey = 'allOfUsApiUrlOverride';
export const overriddenPublicUrlKey = 'publicApiUrlOverride';


@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
              '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit {
  user: Observable<SignInDetails>;
  hasReviewResearchPurpose = false;
  hasReviewIdVerification = false;
  headerImg = '/assets/images/all-of-us-logo.svg';
  sidenavToggle = false;

  @ViewChild('sidenavToggleElement') sidenavToggleElement: ElementRef;

  @ViewChild('sidenav') sidenav: ElementRef;

  @HostListener('document:click', ['$event'])
  onClickOutsideSideNav(event: MouseEvent) {
    const inSidenav = this.sidenav.nativeElement.contains(event.target);
    const inSidenavToggle = this.sidenavToggleElement.nativeElement.contains(event.target);
    if (this.sidenavToggle && !(inSidenav || inSidenavToggle)) {
      this.sidenavToggle = false;
    }
  }
  constructor(
    /* Ours */
    private signInService: SignInService,
    private profileService: ProfileService,
    /* Angular's */
    private locationService: Location,
  ) {}

  ngOnInit(): void {
    document.body.style.backgroundColor = '#f1f2f2';
    this.user = this.signInService.user;
    this.user.subscribe(user => {
      this.profileService.getMe().subscribe(profile => {
        this.hasReviewResearchPurpose =
          profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        this.hasReviewIdVerification =
          profile.authorities.includes(Authority.REVIEWIDVERIFICATION);
          // this.email = profile.username;
      });
    });
  }

  signOut(e: Event): void {
    this.signInService.signOut();
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
