import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import * as fp from 'lodash/fp';
import outdatedBrowserRework from 'outdated-browser-rework';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {Switch, useHistory} from 'react-router-dom';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

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
import {bindApiClients, configApi, getApiBaseUrl, workspacesApi} from 'app/services/swagger-fetch-clients';
import {useIsUserDisabled} from 'app/utils/access-utils';
import {initializeAnalytics} from 'app/utils/analytics';
import {useAuthentication} from 'app/utils/authentication';
import {
  cookiesEnabled,
  LOCAL_STORAGE_API_OVERRIDE_KEY,
  LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN
} from 'app/utils/cookies';
import {ExceededActionCountError, LeoRuntimeInitializer} from 'app/utils/leo-runtime-initializer';
import {
  currentWorkspaceStore,
  nextWorkspaceWarmupStore,
  urlParamsStore
} from 'app/utils/navigation';
import {
  authStore,
  routeDataStore,
  runtimeStore,
  serverConfigStore,
  stackdriverErrorReporterStore
} from 'app/utils/stores';
import {buildPageTitleForEnvironment} from 'app/utils/title';
import {environment} from 'environments/environment';
import {ConfigResponse, Configuration} from 'generated/fetch';
import 'rxjs/Rx';

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

const ScrollToTop = () => {
  const {location} = useHistory();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location]);

  return <React.Fragment/>;
};

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = () => {
  const {authLoaded} = useAuthentication();
  const isUserDisabled = useIsUserDisabled();
  const [pollAborter, setPollAborter] = useState(new AbortController());
  const [, setTestAccessTokenOverride] = useState(undefined);
  const [isCookiesEnabled, setIsCookiesEnabled] = useState(false);
  const [overriddenUrl, setOverriddenUrl] = useState('');

  useEffect(() => {
    // TODO angular2react - is it better to pull this out into a const so this loop only runs once?
    // TODO angular2react - this actually isn't working right now, just renders an empty page
    // but this bug is also on test right now so it isn't a regression
    setIsCookiesEnabled(cookiesEnabled());

    if (isCookiesEnabled) {
      try {
        setOverriddenUrl(localStorage.getItem(LOCAL_STORAGE_API_OVERRIDE_KEY));

        window['setAllOfUsApiUrl'] = (url: string) => {
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
        console.log('To override the API URLs, try:\n' +
          'setAllOfUsApiUrl(\'https://host.example.com:1234\')');
      } catch (err) {
        console.log('Error setting urls: ' + err);
      }
    }
  }, [isCookiesEnabled]);

  useEffect(() => {
    checkBrowserSupport();
  }, []);

  // TODO angular2react - this might need to go into main.ts now since useEffect runs fairly late
  useEffect(() => {
    // Set this as early as possible in the application boot-strapping process,
    // so it's available for Puppeteer to call. If we need this even earlier in
    // the page, it could go into something like main.ts, but ideally we'd keep
    // this logic in one place, and keep main.ts minimal.
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
  }, []);

  useEffect(() => {
    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    document.title = buildPageTitleForEnvironment();
    routeDataStore.subscribe(({title, pathElementForTitle}) => {
      document.title = buildPageTitleForEnvironment(title || urlParamsStore.getValue()[pathElementForTitle]);
    });
  }, []);

  const currentAccessToken = () => {
    const tokenOverride = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);

    // TODO angular2react - this used to be a setState() variable but I had to switch it to read from
    // localStorage because testAccessTokenOverride would not be set yet in the first run of this and
    // configure the API clients incorrectly. this should fix the issue but I want to think more about
    // what the correct solution is. Getting rid of the state variable and only reading from localStorage
    // could be the way to go.
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

  const serverConfigStoreCallback = (config: ConfigResponse) => {
    // TODO angular2react - is this the right place for this?
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

    // Enable test access token override via local storage. Intended to support
    // Puppeteer testing flows. This is handled in the server config callback
    // for signin timing consistency. Normally we cannot sign in until we've
    // loaded the oauth client ID from the config service.
    if (environment.allowTestAccessTokenOverride) {
      const localStorageTestAccessToken = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
      // TODO angular2react - can I just replace this with `setTestAccessTokenOverride(window....)` and assume that
      // the right value will be used in the if conditional in the same execution loop?
      setTestAccessTokenOverride(localStorageTestAccessToken);
      if (localStorageTestAccessToken) {
        console.log('found test access token in local storage, skipping normal auth flow');

        // The client has already configured an access token override. Skip the normal oauth flow.
        authStore.set({...authStore.get(), authLoaded: true, isSignedIn: true});
        return;
      }
    }
  };

  useEffect(() => {
    // We only want to run this callback once. Either run it now or subscribe and run it later when we
    // get the config.
    const serverConfig = serverConfigStore.get();
    if (serverConfig.config) {
      serverConfigStoreCallback(serverConfig.config);
    } else {
      // This is making the assumption that a value received from the serverConfigStore will be a valid config
      const {unsubscribe} = serverConfigStore.subscribe((nextServerConfig) => {
        unsubscribe();
        serverConfigStoreCallback(nextServerConfig.config);
      });
    }
  }, []);

  const loadConfig = async() => {
    const config = await configApi().getConfig();
    serverConfigStore.set({config: config});
  };

  useEffect(() => {
    const load = async() => {
      await loadConfig();
      loadErrorReporter();
      initializeAnalytics();
    };

    load();
  }, []);

  useEffect(() => {
    const sub = urlParamsStore
      .map(({ns, wsid}) => ({ns, wsid}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(async({ns, wsid}) => {
        currentWorkspaceStore.next(null);
        // This needs to happen for testing because we seed the urlParamsStore with {}.
        // Otherwise it tries to make an api call with undefined, because the component
        // initializes before we have access to the route.
        if (!ns || !wsid) {
          return null;
        }

        // In a handful of situations - namely on workspace creation/clone,
        // the application will preload the next workspace to avoid a redundant
        // refetch here.
        const nextWs = nextWorkspaceWarmupStore.getValue();
        nextWorkspaceWarmupStore.next(undefined);
        if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
          return nextWs;
        }

        // TODO angular2react : do we really need this hack?
        // Hack to ensure auth is loaded before a workspaces API call.
        // await this.signInService.isSignedIn$.first().toPromise();

        return await workspacesApi().getWorkspace(ns, wsid).then((wsResponse) => {
          return {
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel
          };
        });
      })
      .subscribe(async(workspace) => {
        if (workspace === null) {
          // This handles the empty urlParamsStore story.
          return;
        }
        currentWorkspaceStore.next(workspace);
        runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});
        pollAborter.abort();
        const newPollAborter = new AbortController();
        setPollAborter(newPollAborter);
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: workspace.namespace,
            pollAbortSignal: newPollAborter.signal,
            maxCreateCount: 0,
            maxDeleteCount: 0,
            maxResumeCount: 0
          });
        } catch (e) {
          // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
          // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
          // initialization here.
          if (!(e instanceof ExceededActionCountError)) {
            throw e;
          }
        }
      });

    return sub.unsubscribe;
  }, []);

  useEffect(() => {
    return urlParamsStore
      .map(({ns, wsid}) => ({ns, wsid}))
      .debounceTime(1000) // Kind of hacky but this prevents multiple update requests going out simultaneously
      // due to urlParamsStore being updated multiple times while rendering a route.
      // What we really want to subscribe to here is an event that triggers on navigation start or end
      // Debounce 1000 (ms) will throttle the output events to once a second which should be OK for real life usage
      // since multiple update recent workspace requests (from the same page) within the span of 1 second should
      // almost always be for the same workspace and extremely rarely for different workspaces
      .subscribe(({ns, wsid}) => {
        if (ns && wsid) {
          workspacesApi().updateRecentWorkspaces(ns, wsid);
        }
      }).unsubscribe;
  }, []);

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
        {/* TODO angular2react: rendering component through component() prop is causing the components to unmount/remount on every render*/}
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
              <img alt='logo' src='/assets/images/all-of-us-logo.svg' width='155px'/>
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
