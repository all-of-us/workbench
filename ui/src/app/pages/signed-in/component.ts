import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {SignInService} from 'app/services/sign-in.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {Subscription} from 'rxjs/Subscription';
import {INACTIVITY_CONFIG} from './inactivity-monitor';

import {FooterTypeEnum} from 'app/components/footer';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {setInstitutionCategoryState} from 'app/utils/analytics';
import {navigateSignOut, routeConfigDataStore} from 'app/utils/navigation';
import {
  cdrVersionStore,
  compoundRuntimeOpStore,
  profileStore,
  routeDataStore,
  serverConfigStore
} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';

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
export class SignedInComponent implements OnInit, OnDestroy {
  profile: Profile;
  enableSignedInFooter = environment.enableFooter;
  minimizeChrome = false;
  cdrVersionsInitialized = false;
  serverConfigInitialized = false;
  private signOutNavigateSub: Subscription;
  private subscriptions = [];

  footerTypeEnum = FooterTypeEnum;

  @ViewChild('sidenavToggleElement') sidenavToggleElement: ElementRef;

  @ViewChild('sidenav') sidenav: ElementRef;

  constructor(
    private signInService: SignInService
  ) {
    this.signOut = this.signOut.bind(this);
  }

  async ngOnInit() {
    // We want to block app rendering on the presence of server config
    // data so that route components don't try to lookup config data
    // before it's available.
    // This will need to be a step in the React bootstrapping as well.
    // See discussion on https://github.com/all-of-us/workbench/pull/4713
    if (serverConfigStore.get().config) {
      // This particular callback doesn't care about what's in the server
      // config store, only that it's populated.
      this.serverConfigStoreCallback();
    } else {
      const {unsubscribe} = serverConfigStore.subscribe(() => {
        unsubscribe();
        this.serverConfigStoreCallback();
      });
    }

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

    window.addEventListener('beforeunload', checkOpsBeforeUnload);
  }

  private async serverConfigStoreCallback() {
    this.serverConfigInitialized = true;
    this.profile = await profileStore.get().load();
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
  }

  signOut(continuePath?: string): void {
    window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, null);
    // Unsubscribe from our standard signout navigation handler before signing out, so we can handle
    // different navigation scenarios explicitly within this method.
    this.signOutNavigateSub.unsubscribe();
    this.signInService.signOut();
    navigateSignOut(continuePath);
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
    window.removeEventListener('beforeunload', checkOpsBeforeUnload);
  }
}
