import {Location} from '@angular/common';
import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {
  Router,
} from '@angular/router';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {BugReportComponent} from 'app/views/bug-report/component';

import {Authority} from 'generated';

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
    private signInService: SignInService,
    private profileStorageService: ProfileStorageService,
    /* Angular's */
    private locationService: Location,
    private router: Router,
  ) {}

  ngOnInit(): void {

    this.profileStorageService.profile$.subscribe((profile) => {
      this.hasReviewResearchPurpose =
        profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
      this.hasReviewIdVerification =
        profile.authorities.includes(Authority.REVIEWIDVERIFICATION);
      this.givenName = profile.givenName;
      this.familyName = profile.familyName;
    });

    document.body.style.backgroundColor = '#f1f2f2';
    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (signedIn) {
        this.profileImage = this.signInService.profileImage;
        this.profileStorageService.reload();
      } else {
        const params = {};
        const url = this.router.routerState.snapshot.url;
        if (url && url !== '/') {
          params['from'] = url;
        }
        this.router.navigate(['/login', params]);
      }
    });
  }

  signOut(): void {
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
