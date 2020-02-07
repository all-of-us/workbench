import {Location} from '@angular/common';
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';

import {debouncer, hasRegisteredAccessFetch} from 'app/utils';
import {cdrVersionStore, navigateSignOut, routeConfigDataStore} from 'app/utils/navigation';
import {initializeZendeskWidget} from 'app/utils/zendesk';
import {environment} from 'environments/environment';
import {Profile as FetchProfile} from 'generated/fetch';
import Timeout = NodeJS.Timeout;

/*
 * The user's last known active timestamp is stored in localStorage with the key of
 * INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE. This value is checked whenever
 * the application is reloaded. If the difference between the time at reload and the
 * value in local storage is greater than the inactivity timeout period, the user will
 * be signed out of all Google accounts.
 *
 * If the localStorage value is null for whatever reason, we defer to the more secure
 * solution of logging out the user. This should not affect new users since the logout
 * flow is ignored if there is no user session.
 */
export const INACTIVITY_CONFIG = {
  TRACKED_EVENTS: ['mousedown', 'keypress', 'scroll', 'click'],
  LOCAL_STORAGE_KEY_LAST_ACTIVE: 'LAST_ACTIVE_TIMESTAMP_EPOCH_MS',
  MESSAGE_KEY: 'USER_ACTIVITY_DETECTED'
};

@Component({
  selector: 'app-signed-in',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/errors.css'],
  templateUrl: './component.html'
})
export class SignedInComponent implements OnInit, OnDestroy, AfterViewInit {
  profile: FetchProfile;
  headerImg = '/assets/images/all-of-us-logo.svg';
  displayTag = environment.displayTag;
  shouldShowDisplayTag = environment.shouldShowDisplayTag;
  minimizeChrome = false;
  // True if the user tried to open the Zendesk support widget and an error
  // occurred.
  zendeskLoadError = false;
  showInactivityModal = false;
  private profileLoadingSub: Subscription;
  private subscriptions = [];

  private getUserActivityTimer: () => Timeout;
  private inactivityInterval: Timeout;
  private logoutTimer: Timeout;
  private inactivityModalTimer: Timeout;

  @ViewChild('sidenavToggleElement') sidenavToggleElement: ElementRef;

  @ViewChild('sidenav') sidenav: ElementRef;

  constructor(
    /* Ours */
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
        this.profile = profile as unknown as FetchProfile;
        if (hasRegisteredAccessFetch(this.profile.dataAccessLevel)) {
          cdrVersionsApi().getCdrVersions().then(resp => {
            cdrVersionStore.next(resp);
          });
        }
      });
    });

    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (!signedIn) {
        this.signOut();
      }
    });

    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = minimizeChrome;
    }));

    this.signOutIfLocalStorageInactivityElapsed();

    this.startUserActivityTracker();
    this.startInactivityMonitoring();
  }

  private getInactivityElapsedMs(): number {
    const lastActive = window.localStorage.getItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE);
    if (!lastActive) {
      return null;
    }
    return Date.now() - parseInt(lastActive, 10);
  }

  private signOutIfLocalStorageInactivityElapsed(): void {
    const elapsedMs = this.getInactivityElapsedMs();
    if (elapsedMs && elapsedMs > environment.inactivityTimeoutSeconds * 1000) {
      this.signOut();
    }
  }

  signOut(): void {
    window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, null);
    this.signInService.signOut();
    navigateSignOut();
  }

  startUserActivityTracker() {
    const signalUserActivity = debouncer(() => {
      window.postMessage(INACTIVITY_CONFIG.MESSAGE_KEY, '*');
    }, 1000);
    this.getUserActivityTimer = signalUserActivity.getTimer;

    INACTIVITY_CONFIG.TRACKED_EVENTS.forEach(eventName => {
      window.addEventListener(eventName, () => signalUserActivity.invoke(), false);
    });
  }

  private startInactivityTimers(elapsedMs: number = 0) {
    clearTimeout(this.logoutTimer);
    this.logoutTimer = setTimeout(
      () => this.signOut(),
      Math.max(0, environment.inactivityTimeoutSeconds * 1000 - elapsedMs));

    clearTimeout(this.inactivityModalTimer);
    this.inactivityModalTimer = setTimeout(
      () => this.showInactivityModal = true,
      Math.max(0, 1000 * (environment.inactivityTimeoutSeconds - environment.inactivityWarningBeforeSeconds) - elapsedMs));
  }

  startInactivityMonitoring() {
    this.startInactivityTimers();

    const hideModal = () => this.showInactivityModal = false;
    const resetTimers = () => {
      hideModal();
      this.startInactivityTimers();
    };

    localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
    resetTimers();

    // setTimeout does not necessary track real wall-time. Periodically
    // clear/restart the timers so that they reflect the time which has elapsed
    // since we last saw activity, as tracked in local storage.
    this.inactivityInterval = setInterval(() => {
      this.startInactivityTimers(this.getInactivityElapsedMs());
    }, 60 * 1000);

    window.addEventListener('message', (e) => {
      if (e.data !== INACTIVITY_CONFIG.MESSAGE_KEY) {
        return;
      }

      // setTimeout is not a reliable mechanism for forcing signout as it doesn't
      // model actual wall-time, for example a sleeping machine does not progress
      // a setTimeout timer. Always check whether the user's time has already
      // elapsed before updating our inactivity time tracker.
      this.signOutIfLocalStorageInactivityElapsed();

      window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, Date.now().toString());
      resetTimers();
    }, false);

    window.addEventListener('storage', (e) => {
      if (e.key === INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE && e.newValue !== null) {
        this.signOutIfLocalStorageInactivityElapsed();
        resetTimers();
      }
    });
  }

  ngAfterViewInit() {
    // If we ever get a test Zendesk account,
    // the key should be templated in using an environment variable.
    initializeZendeskWidget(this.elementRef, environment.zendeskWidgetKey);
  }

  ngOnDestroy() {
    clearInterval(this.getUserActivityTimer());
    clearInterval(this.inactivityInterval);
    clearTimeout(this.logoutTimer);
    clearTimeout(this.inactivityModalTimer);

    if (this.profileLoadingSub) {
      this.profileLoadingSub.unsubscribe();
    }
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  closeInactivityModal(): void {
    this.showInactivityModal = false;
  }

  secondsToText(seconds) {
    return seconds % 60 === 0 && seconds > 60 ?
      `${seconds / 60} minutes` : `${seconds} seconds`;
  }

  get inactivityModalText(): string {
    const secondsBeforeDisplayingModal =
      environment.inactivityTimeoutSeconds - environment.inactivityWarningBeforeSeconds;

    return `You have been idle for over ${this.secondsToText(secondsBeforeDisplayingModal)}. ` +
      `You can choose to extend your session by clicking the button below. You will be automatically logged ` +
      `out if there is no action in the next ${this.secondsToText(environment.inactivityWarningBeforeSeconds)}.`;
  }

  get bannerAdminActive(): boolean {
    return this.locationService.path() === '/admin/banner';
  }

  get userAdminActive(): boolean {
    return this.locationService.path() === '/admin/user';
  }

  get workspaceAdminActive(): boolean {
    return this.locationService.path() === '/admin/workspace';
  }

  get homeActive(): boolean {
    return this.locationService.path() === '';
  }

  get libraryActive(): boolean {
    return this.locationService.path() === '/library';
  }

  get workspacesActive(): boolean {
    return this.locationService.path() === '/workspaces';
  }

  get profileActive(): boolean {
    return this.locationService.path() === '/profile';
  }
}
