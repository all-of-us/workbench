import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import * as fp from 'lodash/fp';
import outdatedBrowserRework from 'outdated-browser-rework';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {Switch} from 'react-router-dom';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {AppRoute, AppRouter, Guard, ProtectedRoutes, withRouteData} from 'app/components/app-router';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {SignedIn} from 'app/pages/signed-in/signed-in';
import {UserDisabled} from 'app/pages/user-disabled';
import {useIsUserDisabled} from 'app/utils/access-utils';
import {
  authStore,
  routeDataStore,
  runtimeStore,
  serverConfigStore,
  stackdriverErrorReporterStore,
  useStore
} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {ConfigResponse, Configuration} from 'generated/fetch';
import 'rxjs/Rx';
import {NotificationModal} from './components/modals';
import {SignIn} from './pages/login/sign-in';
import {bindApiClients, configApi, getApiBaseUrl, workspacesApi} from './services/swagger-fetch-clients';
import {AnalyticsTracker, initializeAnalytics, setLoggedInState} from './utils/analytics';
import {cookiesEnabled, LOCAL_STORAGE_API_OVERRIDE_KEY} from './utils/cookies';
import {ExceededActionCountError, LeoRuntimeInitializer} from './utils/leo-runtime-initializer';
import {
  currentWorkspaceStore,
  nextWorkspaceWarmupStore,
  signInStore,
  urlParamsStore
} from './utils/navigation';
import {buildPageTitleForEnvironment} from './utils/title';

declare const gapi: any;

// for e2e tests: provide your own oauth token to obviate Google's oauth UI
// flow, thereby avoiding inevitable challenges as Google identifies Puppeteer
// as non-human.
declare global {
  interface Window { setTestAccessTokenOverride: (token: string) => void; }
}

const LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN = 'test-access-token-override';

const signInGuard: Guard = {
  allowed: (): boolean => {
    console.log(authStore.get().isSignedIn);
    return authStore.get().isSignedIn;
  },
  redirectPath: '/login'
};

const disabledGuard = (userDisabled: boolean): Guard => ({
  allowed: (): boolean => !userDisabled,
  redirectPath: '/user-disabled'
});

const CookiePolicyPage = fp.flow(withRouteData, withRoutingSpinner)(CookiePolicy);
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

function clearIdToken(): void {
  // Using the full page redirect puts a long "id_token" parameter in the
  // window hash; clear this after gapi has consumed it.
  window.location.hash = '';
}

function profileImage() {
  if (!gapi.auth2) {
    return null;
  } else {
    return gapi.auth2.getAuthInstance().currentUser.get().getBasicProfile().getImageUrl();
  }
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

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = () => {
  const {authLoaded = false} = useStore(authStore);
  const isUserDisabled = useIsUserDisabled();
  const [pollAborter, setPollAborter] = useState(new AbortController());
  const [isSignedIn, setIsSignedIn] = useState(false);
  const [testAccessTokenOverride, setTestAccessTokenOverride] = useState(undefined);
  const [signInMounted, setSignInMounted] = useState(false);
  const [doSignOut, setDoSignOut] = useState(false);
  const [isCookiesEnabled, setIsCookiesEnabled] = useState(false);
  const [overriddenUrl, setOverriddenUrl] = useState('');

  // TODO angular2react - does this work?
  const onSignIn = (): void => {
    setSignInMounted(true);
  };

  const signIn = (): void => {
    AnalyticsTracker.Registration.SignIn();

    gapi.auth2.getAuthInstance().signIn({
      'prompt': 'select_account',
      'ux_mode': 'redirect',
      'redirect_uri': `${window.location.protocol}//${window.location.host}`
    });
  };

  const signOut = (): void => {
    gapi.auth2.getAuthInstance().signOut();
  };

  const subscribeToInactivitySignOut = () => {
    setDoSignOut(true);
  };

  const nextSignInStore = () => {
    signInStore.next({
      signOut: () => signOut(),
      profileImage: profileImage(),
    });
  };

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
    routeDataStore.subscribe(({title}) => {
      document.title = buildPageTitleForEnvironment(title);
    });
  }, []);

  useEffect(() => {
    // TODO angular2react - verify that this only runs AFTER subscribeToAuth2User
    // this might run before if react hooks run useEffect on the initial value, otherwise, we should be ok
    // The main thing this affects in the value of authLoaded being true. The rest of the fields make sense
    // with both values of isSignedIn
    console.log('Running useEffect authLoaded ', authLoaded, isSignedIn);
    authStore.set({...authStore.get(), isSignedIn});
    if (isSignedIn) {
      nextSignInStore();
      clearIdToken();

      // TODO angular2react - does this work? or rather, do we even need this?
      if (signInMounted) {
        console.log('Calling redirect to root');
        // <Redirect to='/'/>;
      }
    } else {
      // TODO angular2react - do I really need to check this? when would I ever want to not sign out here
      if (doSignOut) {
        signOut();
      }
    }
    setLoggedInState(isSignedIn);
  }, [isSignedIn]);

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
            authLoaded: true,
            isSignedIn: gapi.auth2.getAuthInstance().isSignedIn.get()
          });
          setIsSignedIn(gapi.auth2.getAuthInstance().isSignedIn.get());

          console.log('Setting up gapi auth listener', gapi.auth2.getAuthInstance().isSignedIn.get());
          gapi.auth2.getAuthInstance().isSignedIn.listen((nextIsSignedIn: boolean) => {
            setIsSignedIn(nextIsSignedIn);
          });
        });
        resolve(gapi.auth2);
      });
    });
  };

  const currentAccessToken = () => {
    if (testAccessTokenOverride) {
      return testAccessTokenOverride;
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
        setIsSignedIn(true);
        return;
      }
    }

    // TODO angular2react - is this the right place for this?
    const conf = new Configuration({
      basePath: getApiBaseUrl(),
      accessToken: () => currentAccessToken()
    });
    bindApiClients(conf);
    notebooksBindApiClients(conf);

    makeAuth2(config);
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
        console.log('Running urlParamsStore sub to get workspace', ns, wsid);

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
        setPollAborter(new AbortController());
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: workspace.namespace,
            pollAbortSignal: pollAborter.signal,
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
        console.log('Runnign update recent workspace ', ns);
        if (ns && wsid) {
          workspacesApi().updateRecentWorkspaces(ns, wsid);
        }
      }).unsubscribe;
  }, []);

  console.log('Rendering AppRouting: ', authLoaded, isUserDisabled);

  return authLoaded && isUserDisabled !== undefined && <React.Fragment>
    {/* Once Angular is removed the app structure will change and we can put this in a more appropriate place */}
    <NotificationModal/>
    {
      isCookiesEnabled && <AppRouter>
        {/* Previously, using a top-level Switch with AppRoute and ProtectedRoute has caused bugs: */}
        {/* see https://github.com/all-of-us/workbench/pull/3917 for details. */}
        {/* It should be noted that the reason this is currently working is because Switch only */}
        {/* duck-types its children; it cares about them having a 'path' prop but doesn't validate */}
        {/* that they are a Route or a subclass of Route. */}
        {/* TODO angular2react: rendering component through component() prop is causing the components to unmount/remount on every render*/}
          <Switch>
              <AppRoute
                  path='/cookie-policy'
                  component={() => <CookiePolicyPage routeData={{title: 'Cookie Policy'}}/>}
              />
              <AppRoute
                  path='/login'
                  component={() => <SignInPage routeData={{title: 'Sign In'}} onSignIn={onSignIn} signIn={signIn}/>}
              />
              <AppRoute
                  path='/session-expired'
                  component={() => <SessionExpiredPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
              />
              <AppRoute
                  path='/sign-in-again'
                  component={() => <SignInAgainPage routeData={{title: 'You have been signed out'}} signIn={signIn}/>}
              />
              <AppRoute
                  path='/user-disabled'
                  component={() => <UserDisabledPage routeData={{title: 'Disabled'}}/>}
              />
              <ProtectedRoutes guards={[signInGuard, disabledGuard(isUserDisabled)]}>
                  <AppRoute
                      path=''
                      exact={false}
                      component={() => <SignedInPage
                        intermediaryRoute={true}
                        routeData={{}}
                        // TODO angular2react - I think I might be able to just sign out and ignore this field
                        subscribeToInactivitySignOut={subscribeToInactivitySignOut}
                        signOut={signOut}
                      />}
                  />
              </ProtectedRoutes>
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
