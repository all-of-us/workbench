import 'rxjs/Rx';

import {
  AppRoute,
  AppRouter,
  withRouteData
} from 'app/components/app-router';
import {NotificationModal} from 'app/components/modals';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {SignIn} from 'app/pages/login/sign-in';
import {NotFound} from 'app/pages/not-found';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {SignedIn} from 'app/pages/signed-in/signed-in';
import {UserDisabled} from 'app/pages/user-disabled';
import {disabledGuard, signInGuard} from 'app/routing/guards';
import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import {bindApiClients, configApi, getApiBaseUrl} from 'app/services/swagger-fetch-clients';
import {useIsUserDisabled} from 'app/utils/access-utils';
import {initializeAnalytics} from 'app/utils/analytics';
import {useAuthentication} from 'app/utils/authentication';
import {
  cookiesEnabled,
  LOCAL_STORAGE_API_OVERRIDE_KEY,
  LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN
} from 'app/utils/cookies';
import {
  authStore,
  serverConfigStore,
  stackdriverErrorReporterStore,
  useStore
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import logo from 'assets/images/all-of-us-logo.svg';
import {environment} from 'environments/environment';
import {Configuration} from 'generated/fetch';
import * as fp from 'lodash/fp';
import outdatedBrowserRework from 'outdated-browser-rework';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {Switch, useHistory} from 'react-router-dom';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

declare const gapi: any;

const CookiePolicyPage = fp.flow(withRouteData, withRoutingSpinner)(CookiePolicy);
const NotFoundPage = fp.flow(withRouteData, withRoutingSpinner)(NotFound);
const SessionExpiredPage = fp.flow(withRouteData, withRoutingSpinner)(SessionExpired);
const SignedInPage = fp.flow(withRouteData, withRoutingSpinner)(SignedIn);
const SignInAgainPage = fp.flow(withRouteData, withRoutingSpinner)(SignInAgain);
const SignInPage = fp.flow(withRouteData, withRoutingSpinner)(SignIn);
const UserDisabledPage = fp.flow(withRouteData, withRoutingSpinner)(UserDisabled);

interface RoutingProps {
  onSignIn: () => void;
  signIn: () => void;
  subscribeToInactivitySignOut: () => void;
  signOut: () => void;
}

const checkBrowserSupport = () => {
  const minChromeVersion = 67;

  outdatedBrowserRework({
    browserSupport: {
      Chrome: minChromeVersion, // Includes Chrome for mobile devices
      Edge: false,
      Safari: false,
      'Mobile Safari': false,
      Opera: false,
      Firefox: false,
      Vivaldi: false,
      IE: false
    },
    isUnknownBrowserOK: false,
    messages: {
      en: {
        outOfDate: 'Researcher Workbench may not function correctly in this browser.',
        update: {
          web: `If you experience issues, please install Google Chrome \
            version ${minChromeVersion} or greater.`,
          googlePlay: 'Please install Chrome from Google Play',
          appStore: 'Please install Chrome from the App Store'
        },
        url: 'https://www.google.com/chrome/',
        callToAction: 'Download Chrome now',
        close: 'Close'
      }
    }
  });
};

const currentAccessToken = () => {
  const tokenOverride = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);

  if (tokenOverride) {
    return tokenOverride;
  } else if (!gapi.auth2) {
    return null;
  } else {
    const authResponse = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse(true);
    if (authResponse !== null) {
      return authResponse.access_token;
    } else {
      return null;
    }
  }
};

const bindClients = () => {
  bindApiClients(
    new Configuration({
      basePath: getApiBaseUrl(),
      accessToken: () => currentAccessToken()
    })
  );
  notebooksBindApiClients(
    new Configuration({
      basePath: environment.leoApiUrl,
      accessToken: () => currentAccessToken()
    })
  );
};

const loadErrorReporter = () => {
  // We don't report to stackdriver on local servers.
  if (environment.debug) {
    return;
  }
  const reporter = new StackdriverErrorReporter();
  const {config} = serverConfigStore.get();
  if (!config.publicApiKeyForErrorReports) {
    return;
  }
  reporter.start({
    key: config.publicApiKeyForErrorReports,
    projectId: config.projectId,
  });

  stackdriverErrorReporterStore.set(reporter);
};

const exposeAccessTokenSetter = () => {
  // Set this as early as possible in the application boot-strapping process,
  // so it's available for Puppeteer to call. If we need this even earlier in
  // the page, it could go into something like index.ts, but ideally we'd keep
  // this logic in one place, and keep index.ts minimal.
  if (environment.allowTestAccessTokenOverride) {
    window.setTestAccessTokenOverride = (token: string) => {
      // Disclaimer: console.log statements here are unlikely to captured by
      // Puppeteer, since it typically reloads the page immediately after
      // invoking this function.
      if (token) {
        window.localStorage.setItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN, token);
        location.replace('/');
      } else {
        window.localStorage.removeItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
      }
    };
  }
};

const ScrollToTop = () => {
  const {location} = useHistory();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location]);

  return <React.Fragment/>;
};

const useServerConfig = () => {
  const {config} = useStore(serverConfigStore);

  useEffect(() => {
    const load = async () => {
      const serverConfig = await configApi().getConfig();
      serverConfigStore.set({config: serverConfig});
    };

    load();
  }, []);

  return config;
};

const useOverriddenApiUrl = () => {
  const [overriddenUrl, setOverriddenUrl] = useState('');

  useEffect(() => {
    if (cookiesEnabled()) {
      try {
        setOverriddenUrl(localStorage.getItem(LOCAL_STORAGE_API_OVERRIDE_KEY));

        window.setAllOfUsApiUrl = (url: string) => {
          if (url) {
            if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
              throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
            }
            setOverriddenUrl(url);
            localStorage.setItem(LOCAL_STORAGE_API_OVERRIDE_KEY, url);
          } else {
            setOverriddenUrl(null);
            localStorage.removeItem(LOCAL_STORAGE_API_OVERRIDE_KEY);
          }
          window.location.reload();
        };
        /* eslint-disable */
        // This should should only be visible in lower environments.
        console.log('To override the API URLs, try:\n' +
          'setAllOfUsApiUrl(\'https://host.example.com:1234\')');
        /* eslint-enable */
      } catch (err) {
        console.error('Error setting urls: ' + err);
      }
    }
  }, []);

  return overriddenUrl;
};



export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = () => {
  const config = useServerConfig();
  const {authLoaded} = useAuthentication();
  const isUserDisabled = useIsUserDisabled();
  const overriddenUrl = useOverriddenApiUrl();

  const loadLocalStorageAccessToken = () => {
    // Ordinarily this sort of thing would go in authentication.tsx - but setting authStore in there causes
    // an infinite loop
    // Enable test access token override via local storage. Intended to support
    // Puppeteer testing flows. This is handled in the server config callback
    // for signin timing consistency. Normally we cannot sign in until we've
    // loaded the oauth client ID from the config service.
    if (config && environment.allowTestAccessTokenOverride && !authLoaded) {
      const localStorageTestAccessToken = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
      if (localStorageTestAccessToken) {
        // The client has already configured an access token override. Skip the normal oauth flow.
        authStore.set({...authStore.get(), authLoaded: true, isSignedIn: true});
      }
    }
  };

  // TODO(RW-7198): Move most of this bootstrapping out of the app router.
  useEffect(() => {
    exposeAccessTokenSetter();
    checkBrowserSupport();
  }, []);

  useEffect(() => {
    if (config) {
      // Bootstrapping that requires server config
      bindClients();
      loadErrorReporter();
      initializeAnalytics();
      loadLocalStorageAccessToken();
    }
  }, [config]);

  const isCookiesEnabled = cookiesEnabled();

  return authLoaded && isUserDisabled !== undefined && <React.Fragment>
    {/* Once Angular is removed the app structure will change and we can put this in a more appropriate place */}
    <NotificationModal/>
    {
      isCookiesEnabled && <AppRouter>
        <ScrollToTop/>
        {/* Previously, using a top-level Switch with AppRoute and ProtectedRoute has caused bugs: */}
        {/* see https://github.com/all-of-us/workbench/pull/3917 for details. */}
        {/* It should be noted that the reason this is currently working is because Switch only */}
        {/* duck-types its children; it cares about them having a 'path' prop but doesn't validate */}
        {/* that they are a Route or a subclass of Route. */}
        <Switch>
          <AppRoute exact path='/cookie-policy'>
            <CookiePolicyPage routeData={{title: 'Cookie Policy'}}/>
          </AppRoute>
          <AppRoute exact path='/login'>
            <SignInPage routeData={{title: 'Sign In'}}/>
          </AppRoute>
          <AppRoute exact path='/session-expired'>
            <SessionExpiredPage routeData={{title: 'You have been signed out'}}/>
          </AppRoute>
          <AppRoute exact path='/sign-in-again'>
            <SignInAgainPage routeData={{title: 'You have been signed out'}}/>
          </AppRoute>
          <AppRoute exact path='/user-disabled'>
            <UserDisabledPage routeData={{title: 'Disabled'}}/>
          </AppRoute>
          <AppRoute exact path='/not-found'>
            <NotFoundPage routeData={{title: 'Not Found'}}/>
          </AppRoute>
          <AppRoute
              path=''
              exact={false}
              intermediaryRoute={true}
              guards={[signInGuard, disabledGuard(isUserDisabled)]}
          >
            <SignedInPage
                intermediaryRoute={true}
                routeData={{}}
            />
          </AppRoute>
        </Switch>
      </AppRouter>
    }
    {
     overriddenUrl && <div style={{position: 'absolute', top: 0, left: '1rem'}}>
      <span style={{fontSize: '80%', color: 'darkred'}}>
        API URL: {overriddenUrl}
      </span>
     </div>
    }
    {
      !isCookiesEnabled &&
      <div>
        <div style={{maxWidth: '500px', margin: '1rem', fontFamily: 'Montserrat'}}>
          <div>
              <img alt='logo' src={logo} width='155px'/>
          </div>
          <div style={{fontSize: '20pt', color: '#2F2E7E', padding: '1rem 0 1rem 0'}}>Cookies are Disabled</div>
          <div style={{fontSize: '14pt', color: '#000000'}}>
          For full functionality of this site it is necessary to enable cookies.
          Here are the <a href='https://support.google.com/accounts/answer/61416'
                          style={{color: '#2691D0'}}
                          target='_blank'
                          rel='noopener noreferrer'>
          instructions how to enable cookies in your web browser</a>.
          </div>
        </div>
      </div>
    }

    <div id='outdated'/> {/* for outdated-browser-rework */}
  </React.Fragment>;
};
