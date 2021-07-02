import {Spinner} from 'app/components/spinners';

import {NavBar} from './nav-bar'
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';

import {Footer, FooterTypeEnum} from 'app/components/footer';
import {ZendeskWidget} from 'app/components/zendesk-widget';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import Timeout = NodeJS.Timeout;
import {setInstitutionCategoryState} from 'app/utils/analytics';
import {navigateSignOut, routeConfigDataStore} from 'app/utils/navigation';
import {
  cdrVersionStore,
  compoundRuntimeOpStore,
  profileStore,
  useStore,
  routeDataStore,
  serverConfigStore,
  RouteDataStore

} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';

import * as React from 'react';

const {useState, useEffect} = React;


/*
XXX
.create-account__degree-select .p-multiselect-header {
  display: none;
}

*/

const styles = reactStyles({
  signedInBody: {
    backgroundColor: '#f1f2f2',
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
    // Minimum supported width is 1300, this allows 20px for the scrollbar.
    minWidth: '1280px'
  },
  contentContainer: {
    display: 'flex',
    position: 'relative',
    flex: '1 0 auto'
  },
  backgroundImage: {
    backgroundImage: `url('/assets/images/BG-Pattern.png')`,
    backgroundSize: '80px', // half the size of the image
    width: '100%',
    height: '100%',
    zIndex: -1,
    position: 'absolute'
  },
  appContainer: {
    width: '100%',
    paddingRight: '0.6rem',
    paddingLeft: '0.6rem',
    flexGrow: 1,
    // Needed for absolute positioned child elements, e.g. spinner.
    position: 'relative'
  },
  minimizeChrome: {
    paddingLeft: 0,
    paddingRight: 0
  }
});

const checkOpsBeforeUnload = (e) => {
  if (Object.keys(compoundRuntimeOpStore.get()).length > 0) {
    // https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
    e.preventDefault();
    e.returnValue = '';
  }
};

export interface Props extends RouteDataStore {
  children: React.Component[]
}

// xxx: withspinner
export const SignedIn = ({children}: Props) => {
  const [cdrVersionsLoaded, setCdrVersionsLoaded] = useState(false);
  const profileState = useStore(profileStore);
  const {minimizeChrome} = {} as any; // XXX

  useEffect(() => {
    const aborter = new AbortController();
    (async() => {
      const profile = await profileState.load();
      if (hasRegisteredAccess(profile.accessTierShortNames)) {
        const cdrResp = await cdrVersionsApi().getCdrVersionsByTier({signal: aborter.signal});
        cdrVersionStore.set(cdrResp);
      }
      setCdrVersionsLoaded(true);
    })();
    return () => aborter.abort();
  }, []);

  useEffect(() => {
    // XXX: Does this really belong here?
    window.addEventListener('beforeunload', checkOpsBeforeUnload);
    return () => window.removeEventListener('beforeunload', checkOpsBeforeUnload);
  }, []);

  // XXX: setInstitutionCategoryState(this.profile.verifiedInstitutionalAffiliation);

  if (!cdrVersionsLoaded || !profileState.profile) {
    return <Spinner />;
  }

  // TODO:
  // - inactivity modal
  // - compute active route info in nav bar
  // - footer z-index
  return <React.Fragment>
    <NavBar
      headerImg="/assets/images/all-of-us-logo.svg"
      displayTag={environment.displayTag}
      shouldShowDisplayTag={environment.shouldShowDisplayTag}
    />
    <div style={styles.contentContainer}>
      <div style={styles.backgroundImage} />
      <div style={{...styles.appContainer, ...(minimizeChrome ? styles.minimizeChrome : {})}}>
        {children}
      </div>
    </div>
    {!minimizeChrome && environment.enableFooter && <Footer type={FooterTypeEnum.Workbench}/>}
    <ZendeskWidget />
  </React.Fragment>;
};

    // this.signOutNavigateSub = this.signInService.isSignedIn$.subscribe(signedIn => {
    //   if (!signedIn) {
    //     this.signOut();
    //   }
    // });


  // signOut(continuePath?: string): void {
  //   window.localStorage.setItem(INACTIVITY_CONFIG.LOCAL_STORAGE_KEY_LAST_ACTIVE, null);
  //   // Unsubscribe from our standard signout navigation handler before signing out, so we can handle
  //   // different navigation scenarios explicitly within this method.
  //   this.signOutNavigateSub.unsubscribe();
  //   this.signInService.signOut();
  //   navigateSignOut(continuePath);
  // }
//}
