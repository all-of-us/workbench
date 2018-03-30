import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {
  ActivatedRoute,
  Event as RouterEvent,
  Router,
} from '@angular/router';

import {Observable} from 'rxjs/Observable';

import {SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';

import {AppComponent} from '../app/component';

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
  hasReviewResearchPurpose = false;
  hasReviewIdVerification = false;
  headerImg = '/assets/images/all-of-us-logo.svg';
  givenName = '';
  familyName = '';
  profileImage = '';
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
    private appComponent: AppComponent,
    private signInService: SignInService,
    private profileService: ProfileService,
    /* Angular's */
    private locationService: Location,
    private router: Router,
  ) {}

  ngOnInit(): void {

    this.profileImage = this.signInService.profileImage;
    document.body.style.backgroundColor = '#f1f2f2';
    this.profileService.getMe().subscribe(profile => {
      this.givenName = profile.givenName;
      this.familyName = profile.familyName;
    });
    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (signedIn) {
        this.profileService.getMe().subscribe(profile => {
          this.hasReviewResearchPurpose =
            profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
          this.hasReviewIdVerification =
            profile.authorities.includes(Authority.REVIEWIDVERIFICATION);
            // this.email = profile.username;
        });
      } else {
        this.router.navigate(['/login', {
          from: this.router.routerState.snapshot.url
        }]);
      }
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
