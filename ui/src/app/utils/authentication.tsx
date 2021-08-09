import {authStore, serverConfigStore, useStore} from "app/utils/stores";
import {ConfigResponse} from "generated/fetch";
import {environment} from "environments/environment";
import {useEffect} from "react";
import {setLoggedInState} from "./analytics";
import {signInStore} from "./navigation";
import {LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN} from "./cookies";

declare const gapi: any;

/**
 * @name useAuthentication
 * @description React hook that provides the user with the signed-in status of the current user and
 *              handles redirect, etc. as appropriate when that state changes
 */
export function useAuthentication() {
  const {authLoaded, isSignedIn, enableInactivityTimeout} = useStore(authStore);
  const {config} = useStore(serverConfigStore);

  useEffect(() => {
    if (config) {
      makeAuth2(config);
    }
  }, [config]);

  useEffect(() => {
    if (isSignedIn) {
      signInStore.next({
        signOut: () => signOut(),
        profileImage: profileImage(),
      });
      clearIdToken();
    } else {
      if (enableInactivityTimeout) {
        signOut();
      }
    }
    setLoggedInState(isSignedIn);
  }, [isSignedIn]);

  return {authLoaded: authLoaded};
}

const makeAuth2 = (config: ConfigResponse): Promise<any> => {
  return new Promise((resolve) => {
    gapi.load('auth2', () => {
      console.log(config);
      gapi.auth2.init({
        client_id: environment.clientId,
        hosted_domain: config.gsuiteDomain,
        scope: 'https://www.googleapis.com/auth/plus.login openid profile'
            + (config.enableBillingUpgrade ? ' https://www.googleapis.com/auth/cloud-billing' : '')
      }).then(() => {
        console.log("Setting auth loaded, isSignedIn");
        authStore.set({
          ...authStore.get(),
          authLoaded: true,
          isSignedIn: gapi.auth2.getAuthInstance().isSignedIn.get(),
          enableInactivityTimeout: false
        });

        console.log('Setting up gapi auth listener; isSignedIn: ', gapi.auth2.getAuthInstance().isSignedIn.get());
        gapi.auth2.getAuthInstance().isSignedIn.listen((nextIsSignedIn: boolean) => {
          authStore.set({...authStore.get(), isSignedIn: nextIsSignedIn});
        });
      });
      resolve(gapi.auth2);
    });
  });
};

// TODO: When we sign out, we see a flash of the login page before being redirected to the Google signout page. Go directly to Google signout page instead.
export const signOut = (): void => {
  // If we're in puppeteer, we never call gapi.auth2.init, so we can't sign out normally.
  // Instead, we revoke all the access tokens and reset all the state.
  if (environment.allowTestAccessTokenOverride && window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN)) {
    authStore.set({...authStore.get(), authLoaded: true, isSignedIn: false, enableInactivityTimeout: false});
    window.setTestAccessTokenOverride('');
  } else {
    gapi.auth2.getAuthInstance().signOut();
  }
};

function profileImage() {
  if (!gapi.auth2) {
    return null;
  } else {
    return gapi.auth2.getAuthInstance().currentUser.get().getBasicProfile().getImageUrl();
  }
}

function clearIdToken(): void {
  // Using the full page redirect puts a long "id_token" parameter in the
  // window hash; clear this after gapi has consumed it.
  window.location.hash = '';
}
