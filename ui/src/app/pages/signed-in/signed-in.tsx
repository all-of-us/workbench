import * as React from 'react';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {Footer, FooterTypeEnum} from 'app/components/footer';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {ZendeskWidget} from 'app/components/zendesk-widget';
import {InactivityMonitor} from 'app/pages/signed-in/inactivity-monitor';
import {NavBar} from 'app/pages/signed-in/nav-bar';
import {SignedInRoutes} from 'app/routing/signed-in-app-routing';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles} from 'app/utils';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {setInstitutionCategoryState} from 'app/utils/analytics';
import {
  cdrVersionStore,
  compoundRuntimeOpStore,
  profileStore,
  routeDataStore, serverConfigStore, useStore
} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {useEffect, useState} from 'react';

import backgroundImage from 'assets/images/BG-Pattern.png';

const styles = reactStyles({
  appContainer: {
    width: '100%',
    paddingRight: '0.6rem',
    paddingLeft: '0.6rem',
    flexGrow: 1,
    /* Needed for absolute positioned child elements, e.g. spinner. */
    position: 'relative'
  },
  backgroundImage: {
    backgroundImage: `url(${backgroundImage})`,
    backgroundSize: '80px', /* half the size of the image */
    width: '100%',
    height: '100%',
    zIndex: -1,
    position: 'absolute'
  },
});

const checkOpsBeforeUnload = (e) => {
  if (Object.keys(compoundRuntimeOpStore.get()).length > 0) {
    // https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onbeforeunload
    e.preventDefault();
    e.returnValue = '';
  }
};

export const SignedIn = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  const [hideFooter, setHideFooter] = useState(false);

  const {config} = useStore(serverConfigStore);
  const {tiers} = useStore(cdrVersionStore);
  const profileState = useStore(profileStore);

  useEffect(() => {
    window.addEventListener('beforeunload', checkOpsBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', checkOpsBeforeUnload);
    };
  }, []);

  useEffect(() => {
    const subscription = routeDataStore.subscribe(({minimizeChrome}) => {
      setHideFooter(minimizeChrome);
    });

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  useEffect(() => {
    /*
     * TODO RW-6726 We want to block app rendering on the presence of server config data
     *  so that route components don't try to lookup config data before it's available.
     *  We should holistically figure out a Reacty way to do this.
     *  See discussion on https://github.com/all-of-us/workbench/pull/4713
     */
    const checkStoresLoaded = async() => {
      // AppComponent should be loading the server config.
      if (config) {
        if (!profileState.profile) {
          profileState.load();
          return;
        }
        setInstitutionCategoryState(profileState.profile.verifiedInstitutionalAffiliation);
        if (hasRegisteredAccess(profileState.profile.accessTierShortNames)) {
          if (!tiers) {
            const cdrVersionsByTier = await cdrVersionsApi().getCdrVersionsByTier();
            cdrVersionStore.set(cdrVersionsByTier);
            return;
          }
        }
      }
    };

    checkStoresLoaded();
  }, [profileState, tiers]);

  return <FlexColumn
    style={{
      minHeight: '100vh',
      /* minimum supported width is 1300, this allows 20px for the scrollbar */
      minWidth: '1280px'
    }}
    data-test-id='signed-in'
  >
    <NavBar/>
    <FlexRow style={{position: 'relative', flex: '1 0 auto'}}>
      <div
          style={styles.backgroundImage}
      />
      {/* We still want people to be able to access the homepage, etc. even if they shouldn't */}
      {/* know about CDR details; they'll be blocked from other routes by not having access too */}
      {
        config && (
            tiers || (
                profileState.profile &&
                !hasRegisteredAccess(profileState.profile.accessTierShortNames)
            )) && <div
          style={
            hideFooter
                ? {...styles.appContainer, paddingLeft: 0, paddingRight: 0}
                : styles.appContainer
          }
        >
          <SignedInRoutes/>
        </div>
      }
    </FlexRow>
    {!hideFooter && environment.enableFooter &&
      <Footer
          type={FooterTypeEnum.Workbench}
      />
    }
    <InactivityMonitor/>
    <ZendeskWidget/>
  </FlexColumn>;
};
