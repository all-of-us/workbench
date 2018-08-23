import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {
  Router,
} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {hasRegisteredAccess} from 'app/utils';
import {BugReportComponent} from 'app/views/bug-report/component';

import {environment} from 'environments/environment';

import {Authority} from 'generated';

@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/errors.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit {
  hasDataAccess = true;
  hasReviewResearchPurpose = false;
  hasReviewIdVerification = false;
  headerImg = '/assets/images/all-of-us-logo.svg';
  givenName = '';
  familyName = '';
  profileImage = '';
  sidenavToggle = false;
  publicUiUrl = environment.publicUiUrl;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

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
    public errorHandlingService: ErrorHandlingService,
    private signInService: SignInService,
    private serverConfigService: ServerConfigService,
    private profileStorageService: ProfileStorageService,
    /* Angular's */
    private locationService: Location,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.serverConfigService.getConfig().subscribe((config) => {
      this.profileStorageService.profile$.subscribe((profile) => {
        this.hasDataAccess =
          !config.enforceRegistered || hasRegisteredAccess(profile.dataAccessLevel);

        this.hasReviewResearchPurpose =
          profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        this.hasReviewIdVerification =
          profile.authorities.includes(Authority.REVIEWIDVERIFICATION);
        this.givenName = profile.givenName;
        this.familyName = profile.familyName;
      });
    });

    // TODO: Remove or move into CSS.
    document.body.style.backgroundColor = '#f1f2f2';
    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (signedIn) {
        this.profileImage = this.signInService.profileImage;
      } else {
        this.navigateSignOut();
      }
    });
  }

  signOut(): void {
    this.signInService.signOut();
    this.navigateSignOut();
  }

  private navigateSignOut(): void {
    // Force a hard browser reload here. We want to ensure that no local state
    // is persisting across user sessions, as this can lead to subtle bugs.
    window.location.assign('/');
  }


  get reviewWorkspaceActive(): boolean {
    return this.locationService.path() === '/admin/review-workspace';
  }

  get reviewIdActive(): boolean {
    return this.locationService.path() === '/admin/review-id-verification';
  }

  get homeActive(): boolean {
    return this.locationService.path() === '';
  }

  get workspacesActive(): boolean {
    return this.locationService.path() === '/workspaces';
  }

  get createWorkspaceActive(): boolean {
    return this.locationService.path() === '/workspaces/build';
  }

}
