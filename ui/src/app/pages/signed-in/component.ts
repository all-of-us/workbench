import {Location} from '@angular/common';
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';

import {FooterTypeEnum} from 'app/components/footer';
import {debouncer} from 'app/utils';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import Timeout = NodeJS.Timeout;
import {setInstitutionCategoryState} from 'app/utils/analytics';
import {navigateSignOut, routeConfigDataStore} from 'app/utils/navigation';
import {cdrVersionStore, compoundRuntimeOpStore, routeDataStore} from 'app/utils/stores';
import {initializeZendeskWidget} from 'app/utils/zendesk';
import {environment} from 'environments/environment';
import {Profile as FetchProfile} from 'generated/fetch';

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

const checkOpsBeforeUnload = (e) => {
  if (Object.keys(compoundRuntimeOpStore.get()).length > 0) {
    // https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
    e.preventDefault();
    e.returnValue = '';
  }
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
  enableSignedInFooter = environment.enableFooter;
  minimizeChrome = false;
  cdrVersionsInitialized = false;
  // True if the user tried to open the Zendesk support widget and an error
  // occurred.
  zendeskLoadError = false;
  showInactivityModal = false;
  private profileLoadingSub: Subscription;
  private signOutNavigateSub: Subscription;
  private subscriptions = [];

  private getUserActivityTimer: () => Timeout;
  private inactivityInterval: Timeout;
  private logoutTimer: Timeout;
  private inactivityModalTimer: Timeout;

  footerTypeEnum = FooterTypeEnum;

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
        setInstitutionCategoryState(this.profile.verifiedInstitutionalAffiliation);
        if (hasRegisteredAccess(this.profile.accessTierShortNames)) {
          cdrVersionsApi().getCdrVersionsByTier().then(resp => {
            // cdrVersionsInitialized blocks app rendering so that route
            // components don't try to lookup CDR data before it's available.
            // This will need to be a step in the React bootstrapping as well.
            // See discussion on https://github.com/all-of-us/workbench/pull/4713
            cdrVersionStore.set(resp);
            this.cdrVersionsInitialized = true;
          });
        } else {
          this.cdrVersionsInitialized = true;
        }
      });
    });

    this.signOutNavigateSub = this.signInService.isSignedIn$.subscribe(signedIn => {
      if (!signedIn) {
        this.signOut();
      }
    });
    this.subscriptions.push(this.signOutNavigateSub);

    // This handles detection of Angular-based routing data.
    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = minimizeChrome;
    }));

    // This handles detection of React-based routing data. During migrations,
    // we assume React routing data will be set deeper/later in the component
    // hierarchy, therefore it will generally take precendence over React.
    this.subscriptions.push(routeDataStore.subscribe(({minimizeChrome}) => {
      this.minimizeChrome = !!minimizeChrome;
    }));

    // As a special case, if we determine the user is inactive immediately after
    // signing in
    this.signOutIfLocalStorageInactivityElapsed('/sign-in-again');

    this.startUserActivityTracker();
    this.startInactivityMonitoring();

    window.addEventListener('beforeunload', checkOpsBeforeUnload);
  }

  private getInactivityElapsedMs(): number {
    const lastActive = window.localStorage.getItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE);
    if (!lastActive) {
      return null;
    }
    return Date.now() - parseInt(lastActive, 10);
  }

  private signOutIfLocalStorageInactivityElapsed(continuePath?: string): void {
    const elapsedMs = this.getInactivityElapsedMs();
    if (elapsedMs && elapsedMs > environment.inactivityTimeoutSeconds * 1000) {
      this.signOut(continuePath);
    }
  }

  signOut(continuePath?: string): void {
    window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, null);
    // Unsubscribe from our standard signout navigation handler before signing out, so we can handle
    // different navigation scenarios explicitly within this method.
    this.signOutNavigateSub.unsubscribe();
    this.signInService.signOut();
    navigateSignOut(continuePath);
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
      () => this.signOut('/session-expired'),
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
    initializeZendeskWidget(this.elementRef);
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
    window.removeEventListener('beforeunload', checkOpsBeforeUnload);
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
    return this.locationService.path() === '/admin/workspaces';
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
