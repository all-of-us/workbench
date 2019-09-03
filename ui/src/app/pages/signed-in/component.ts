import {Location} from '@angular/common';
import {AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {debouncer, hasRegisteredAccess, resettableTimeout} from 'app/utils';
import {cdrVersionStore, routeConfigDataStore} from 'app/utils/navigation';
import {initializeZendeskWidget, openZendeskWidget} from 'app/utils/zendesk';
import {environment} from 'environments/environment';
import {Authority} from 'generated';

export const INACTIVITY_CONFIG = {
  TRACKED_EVENTS: ['mousemove', 'mousedown', 'keypress', 'scroll', 'click'],
  LOCAL_STORAGE_KEY_LAST_ACTIVE: 'LAST_ACTIVE_TIMESTAMP_EPOCH_MS',
  MESSAGE_KEY: 'USER_ACTIVITY_DETECTED',
  LOCAL_STORAGE_KEY_LOGOUT_METHOD: 'SYSTEM_VERIFIED_LOGGED_OUT'
};

export enum LogoutMethod {
  UNKNOWN,
  AUTO,
  MANUAL
}

@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/errors.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit, OnDestroy, AfterViewInit {
  hasDataAccess = true;
  hasReviewResearchPurpose = false;
  hasAccessModuleAdmin = false;
  headerImg = '/assets/images/all-of-us-logo.svg';
  displayTag = environment.displayTag;
  shouldShowDisplayTag = environment.shouldShowDisplayTag;
  givenName = '';
  familyName = '';
  // The email address of the AoU researcher's Google Account. Note that this
  // address should *not* be used for contact purposes, since the AoU GSuite
  // doesn't provide Gmail!
  aouAccountEmailAddress = '';
  // The researcher's preferred contact email address.
  contactEmailAddress = '';
  profileImage = '';
  sidenavToggle = false;
  publicUiUrl = environment.publicUiUrl;
  minimizeChrome = false;
  // True if the user tried to open the Zendesk support widget and an error
  // occurred.
  zendeskLoadError = false;
  showInactivityModal = false;
  private profileLoadingSub: Subscription;
  private subscriptions = [];

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
    private elementRef: ElementRef
  ) {
    this.closeInactivityModal = this.closeInactivityModal.bind(this);
  }

  ngOnInit(): void {
    this.serverConfigService.getConfig().subscribe((config) => {
      this.profileLoadingSub = this.profileStorageService.profile$.subscribe((profile) => {
        this.hasDataAccess =
          !config.enforceRegistered || hasRegisteredAccess(profile.dataAccessLevel);

        this.hasReviewResearchPurpose =
          profile.authorities.includes(Authority.REVIEWRESEARCHPURPOSE);
        this.hasAccessModuleAdmin =
          profile.authorities.includes(Authority.ACCESSCONTROLADMIN);
        this.givenName = profile.givenName;
        this.familyName = profile.familyName;
        this.aouAccountEmailAddress = profile.username;
        this.contactEmailAddress = profile.contactEmail;
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

    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = minimizeChrome;
    }));

    cdrVersionsApi().getCdrVersions().then(resp => {
      cdrVersionStore.next(resp.items);
    });

    this.startUserActivityTracker();
    this.startInactivityTimers();
  }

  startUserActivityTracker() {
    const signalUserActivity = debouncer(() => {
      window.postMessage(INACTIVITY_CONFIG.MESSAGE_KEY, '*');
    }, 1000);

    INACTIVITY_CONFIG.TRACKED_EVENTS.forEach(eventName => {
      window.addEventListener(eventName, () => signalUserActivity(), false);
    });
  }

  startInactivityTimers() {
    const resetLogoutTimeout = resettableTimeout(() => {
      localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LOGOUT_METHOD, LogoutMethod.AUTO.toString());
      this.signOut();
    }, environment.inactivityTimeoutInSeconds * 1000);

    const resetInactivityModalTimeout = resettableTimeout(() => {
      this.showInactivityModal = true;
    }, (environment.inactivityTimeoutInSeconds - environment.inactivityWarningInSecondsBefore) * 1000);

    localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
    localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LOGOUT_METHOD, LogoutMethod.UNKNOWN.toString());
    resetLogoutTimeout();
    resetInactivityModalTimeout();
    window.addEventListener('message', (e) => {
      if (e.data != INACTIVITY_CONFIG.MESSAGE_KEY) {
        return;
      }

      window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
      resetLogoutTimeout();
      resetInactivityModalTimeout();
    }, false);
  }

  ngAfterViewInit() {
    // If we ever get a test Zendesk account,
    // the key should be templated in using an environment variable.
    initializeZendeskWidget(this.elementRef, '5a7d70b9-37f9-443b-8d0e-c3bd3c2a55e3');
  }

  ngOnDestroy() {
    if (this.profileLoadingSub) {
      this.profileLoadingSub.unsubscribe();
    }
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  onClickSignOut(): void {
    localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LOGOUT_METHOD, LogoutMethod.MANUAL.toString());
    this.signOut();
  }

  signOut(): void {
    this.signInService.signOut();
    this.navigateSignOut();
  }

  private navigateSignOut(): void {
    // Force a hard browser reload here. We want to ensure that no local state
    // is persisting across user sessions, as this can lead to subtle bugs.
    window.location.assign('https://accounts.google.com/logout');
  }

  closeInactivityModal(): void {
    this.showInactivityModal = false;
  }

  get inactivityModalText(): string {
    const timeText = environment.inactivityWarningInSecondsBefore % 60 == 0 && environment.inactivityWarningInSecondsBefore > 60 ?
      `${environment.inactivityWarningInSecondsBefore / 60} minutes` :
      `${environment.inactivityWarningInSecondsBefore} seconds`;

    return `You've been idle for sometime. You will be logged out in ${timeText} if no activity is detected.`;
  }

  get reviewWorkspaceActive(): boolean {
    return this.locationService.path() === '/admin/review-workspace';
  }

  get userAdminActive(): boolean {
    return this.locationService.path() === '/admin/user';
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

  openZendesk(): void {
    openZendeskWidget(this.givenName, this.familyName, this.aouAccountEmailAddress,
      this.contactEmailAddress);
  }

  openHubForum(): void {
    window.open(environment.zendeskHelpCenterUrl, '_blank');
  }

}
