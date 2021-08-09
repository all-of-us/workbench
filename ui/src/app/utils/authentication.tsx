import {authStore, serverConfigStore, useStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated/fetch';
import {useEffect} from 'react';
import {AnalyticsTracker, setLoggedInState} from 'app/utils/analytics';
import {LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN} from 'app/utils/cookies';
import {navigateSignOut} from "./navigation";

declare const gapi: any;

// for e2e tests: provide your own oauth token to obviate Google's oauth UI
// flow, thereby avoiding inevitable challenges as Google identifies Puppeteer
// as non-human.
declare global {
  interface Window { setTestAccessTokenOverride: (token: string) => void; }
}

const makeAuth2 = (config: ConfigResponse): Promise<any> => {
  return new Promise((resolve) => {
    gapi.load('auth2', () => {
      gapi.auth2.init({
        client_id: environment.clientId,
        hosted_domain: config.gsuiteDomain,
        scope: 'https://www.googleapis.com/auth/plus.login openid profile'
            + (config.enableBillingUpgrade ? ' https://www.googleapis.com/auth/cloud-billing' : '')
      }).then(() => {
        authStore.set({
          ...authStore.get(),
          authLoaded: true,
          isSignedIn: gapi.auth2.getAuthInstance().isSignedIn.get()
        });

        gapi.auth2.getAuthInstance().isSignedIn.listen((nextIsSignedIn: boolean) => {
          authStore.set({...authStore.get(), isSignedIn: nextIsSignedIn});
        });
      });
      resolve(gapi.auth2);
    });
  });
};

export const signIn = (): void => {
  AnalyticsTracker.Registration.SignIn();

  gapi.auth2.getAuthInstance().signIn({
    'prompt': 'select_account',
    'ux_mode': 'redirect',
    'redirect_uri': `${window.location.protocol}//${window.location.host}`
  });
};

export const signOut = (): void => {
  authStore.set({...authStore.get(), isSignedIn: false});
}

function clearIdToken(): void {
  // Using the full page redirect puts a long "id_token" parameter in the
  // window hash; clear this after gapi has consumed it.
  window.location.hash = '';
}

/**
 * @name useAuthentication
 * @description React hook that provides the user with the signed-in status of the current user and
 *              handles redirect, etc. as appropriate when that state changes
 */
export function useAuthentication() {
  const {authLoaded, isSignedIn} = useStore(authStore);
  const {config} = useStore(serverConfigStore);

  useEffect(() => {
    if (config && (!environment.allowTestAccessTokenOverride || !window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN))) {
      makeAuth2(config);
    }
  }, [config]);

  useEffect(() => {
    if (isSignedIn) {
      clearIdToken();
    } else if (authLoaded) {
      // If we're in puppeteer, we never call gapi.auth2.init, so we can't sign out normally.
      // Instead, we revoke all the access tokens and reset all the state.
      if (environment.allowTestAccessTokenOverride && window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN)) {
        window.setTestAccessTokenOverride('');
      } else {
        gapi.auth2.getAuthInstance().signOut();
      }
    }
    setLoggedInState(isSignedIn);
  }, [isSignedIn]);

  return {authLoaded: authLoaded};
}
