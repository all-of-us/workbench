import { useEffect, useState } from 'react';
import { AuthProviderProps, useAuth } from 'react-oidc-context';

import { ConfigResponse } from 'generated/fetch';

import { environment } from 'environments/environment';
import { userApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker, setLoggedInState } from 'app/utils/analytics';
import { LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN } from 'app/utils/cookies';
import { navigateSignOut } from 'app/utils/navigation';
import { authStore, serverConfigStore } from 'app/utils/stores';
import { delay } from 'app/utils/subscribable';
import { User, WebStorageStateStore } from 'oidc-client-ts';

import { DEMOGRAPHIC_SURVEY_SESSION_KEY } from './constants';

const GOOGLE_BILLING_SCOPE = 'https://www.googleapis.com/auth/cloud-billing';

// In e2e tests we circumvent Google sign-in because puppeteer could be
// flagged as a bot by Google sign-in.
function getTestAccessTokenOverride() {
  return window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
}

/** Returns true if use access token, this is used by Puppeteer test. */
export const isTestAccessTokenActive = () => {
  return (
    environment.allowTestAccessTokenOverride &&
    getTestAccessTokenOverride() !== null
  );
};

export const makeOIDC = (config: ConfigResponse): AuthProviderProps => {
  // We are using the scopes listed in https://accounts.google.com/.well-known/openid-configuration
  // However, the returned scopes are as follows:
  //   - https://www.googleapis.com/auth/userinfo.email
  //   - openid
  //   - https://www.googleapis.com/auth/userinfo.profile
  // It's not clear why this discrepancy exists. Downstream code (such as our API) should only require the above scopes,
  // since it's not clear how to actually obtain the `email` and `profile` scopes.
  const scopes = ['email', 'openid', 'profile'];
  return {
    authority: 'aou', // Dummy value. Required but unused because metadata is defined below
    client_id: environment.clientId,
    metadata: {
      // Overwrite the token endpoint to use our proxy
      token_endpoint: `${environment.allOfUsApiUrl}/oauth2/token`,
      // Values are manually copied from https://accounts.google.com/.well-known/openid-configuration because
      // openid-connect-ts does not support overriding a single metadata item. We have copied the minimum config
      // necessary for our app to function.
      authorization_endpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
      revocation_endpoint: 'https://oauth2.googleapis.com/revoke',
    },
    redirect_uri: `${window.origin}`,
    prompt: 'select_account consent',
    scope: scopes.join(' '),
    userStore: new WebStorageStateStore({ store: window.localStorage }),
    extraQueryParams: {
      access_type: 'offline', // Provides a refresh token that allows us to mint additional access tokens
      hd: config.gsuiteDomain, // hd = "hosted domain"; Provides a hint for which Google account to use
    },
    // If we revoke one token (access/refresh), the other (refresh/access) is revoked as well.
    // Trying to revoke both triggers an unnecessary failed request.
    // This behavior may be specific to Google's OAuth implementation.
    revokeTokenTypes: ['refresh_token'],
    automaticSilentRenew: true,
    includeIdTokenInSilentRenew: true,
    // This implementation is provided by react-oidc-context. This destroys all query parameters, not just auth-related ones.
    // If we want to access any query parameters after sign in, modify this to delete only auth-related parameters.
    // https://github.com/authts/react-oidc-context#:~:text=A%20working%20implementation%20is%20already%20in%20the%20code
    onSigninCallback: (_user: User | void): void => {
      window.history.replaceState({}, document.title, window.location.pathname);
    },
    // https://github.com/DataBiosphere/terra-ui/blob/356f27342ff44d322b2b52077fa4efb1c5f920ce/src/libs/auth.js#LL49C10-L49C10
    accessTokenExpiringNotificationTimeInSeconds: 330,
  };
};

export const signIn = (): Promise<void> => {
  AnalyticsTracker.Registration.SignIn();
  return authStore.get().auth?.signinRedirect();
};

export const signOut = async (continuePath: string = '/login') => {
  // We perform signout manually instead of using auth.signoutRedirect for a few reasons:
  //   1. The user is only signed out of this google account instead of all accounts on the chrome profile
  //   2. The landing page is our home page instead of google's log out page
  //   3. Google's OIDC implementation does not support certain features, such as post_logout_redirect_uri,
  //      requiring workarounds to use

  // Unlike typical OAuth, we own the account and want them to completely sign in again
  let signOutApiCallSucceeded = true;
  try {
    await userApi().signOut();
  } catch {
    signOutApiCallSucceeded = false;
  }

  // This must be called before removeUser because it pulls tokens from localstorage
  try {
    await authStore.get().auth.revokeTokens();
  } catch (e) {
    // `revokeTokens` can fail if the token has already been revoked.
    // Recover from invalid_token errors to make sure the sign-out process completes successfully.
    if (e.error !== 'invalid_token') {
      throw e;
    }
  }

  // Unlike most promises, I don't think this will reject in typical usage. This is because we are using
  // localstorage for our token storage, which is synchronous. However, if an error does happen, it will put our
  // app into a bad state. See RW-9485.
  await authStore.get().auth.removeUser();

  sessionStorage.removeItem(DEMOGRAPHIC_SURVEY_SESSION_KEY);

  if (signOutApiCallSucceeded) {
    window.location.replace(continuePath);
  } else {
    navigateSignOut(continuePath);
  }
};

export const useAuthentication = () => {
  const auth = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  const signOutWithoutLooping = () => {
    setSigningOut(true);
    signOut().finally(() => {
      setSigningOut(false);
    });
  };

  useEffect(() => {
    // `auth` updates rapidly during sign-out; avoid spamming the sign-out API
    if (signingOut) {
      return;
    }

    // Verify whether a user is using the right email to avoid a blank screen
    // See RW-9487 for adding an informative error message.
    if (!auth.isLoading && auth.isAuthenticated && auth.user?.profile.email) {
      const domain: string = auth.user?.profile.email.split('@')[1];
      if (domain !== serverConfigStore.get().config.gsuiteDomain) {
        signOutWithoutLooping();
        return;
      }
    }

    // Works in addition to silent renewal. Catches the case where a user loads the page when the access token is expired.
    // `return`ing this is required to prevent an infinite loop: https://github.com/authts/react-oidc-context#adding-event-listeners
    // Unfortunately, this function triggers _after_ auth declares the user as unauthenticated. In that case, we
    // return early to allow signinSilent to trigger and re-run this function without disrupting the user.
    const expiredCallback = auth.events.addAccessTokenExpired(() => {
      auth.signinSilent().catch(() => {
        signOutWithoutLooping();
      });
    });
    if (!auth.isLoading && auth.user?.expired && !auth.error) {
      return expiredCallback;
    }

    if (isTestAccessTokenActive()) {
      authStore.set({
        authLoaded: true,
        isSignedIn: true,
        auth,
      });
    } else {
      // When prompting for additional scopes, even in a popup, `auth.isLoading` is true. This check ensures the app
      // continues to render on the workspaces page. This is a fairly brittle fix; We should reconsider this after
      // supporting a second sign-in method, such as RAS, which will need to separately address obtaining the billing
      // info scope.
      const loadingBillingScope = auth.activeNavigator === 'signinPopup';
      authStore.set({
        authLoaded:
          (!auth.isLoading && auth.activeNavigator === undefined) ||
          loadingBillingScope,
        isSignedIn: auth.isAuthenticated,
        auth,
      });
    }

    setLoggedInState(authStore.get().isSignedIn);

    if (auth.error) {
      signOutWithoutLooping();
    }

    return expiredCallback;
  }, [auth]);
};

// The delay before continuing to avoid errors due to delays in applying the new scope grant
const BILLING_SCOPE_DELAY_MS = 2000;

export const getAccessToken = () => {
  if (isTestAccessTokenActive()) {
    return getTestAccessTokenOverride();
  } else {
    return authStore.get().auth?.user?.access_token || null;
  }
};

export const hasBillingScope = () => {
  // If uses access token, assume users always have billing scope. The token generated by GenerateImpersonatedUserTokens tool sets billing
  // scope.
  return (
    isTestAccessTokenActive() ||
    authStore.get().auth?.user?.scopes.includes(GOOGLE_BILLING_SCOPE)
  );
};

/*
 * Request Google Cloud Billing scope if necessary.
 *
 * Requesting additional scopes should invoke a browser pop-up which the browser might block.
 * If you use ensureBillingScope during page load and the pop-up is blocked, a rejected promise will
 * be returned. In this case, you'll need to provide something for the user to deliberately click on
 * and retry ensureBillingScope in reaction to the click.
 */
export const ensureBillingScope = async () => {
  if (!hasBillingScope()) {
    await authStore.get().auth?.signinPopup({
      // TODO: Remove this @ts-ignore when https://github.com/authts/oidc-client-ts/pull/866 is released
      // @ts-ignore
      scope: `${authStore.get().auth?.settings.scope} ${GOOGLE_BILLING_SCOPE}`,
    });
    // Wait 250ms before continuing to avoid errors due to delays in applying the new scope grant
    await delay(BILLING_SCOPE_DELAY_MS);
  }
};
