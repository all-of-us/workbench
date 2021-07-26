import {Component as AComponent} from '@angular/core';
import * as fp from 'lodash/fp';
import {useEffect} from 'react';
import * as React from 'react';
import {Redirect} from 'react-router';
import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import {Switch} from 'react-router-dom';

import {AppRoute, AppRouter, Guard, ProtectedRoutes, withRouteData} from 'app/components/app-router';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {SignedIn} from 'app/pages/signed-in/signed-in';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import {ReactWrapperBase} from 'app/utils';
import {useIsUserDisabled} from 'app/utils/access-utils';
import {authStore, serverConfigStore, useStore} from 'app/utils/stores';
import {Subscription} from 'rxjs/Subscription';
import {environment} from '../environments/environment';
import {ConfigResponse, Configuration} from '../generated/fetch';
import {NotificationModal} from './components/modals';
import {SignIn} from './pages/login/sign-in';
import {bindApiClients, configApi, getApiBaseUrl} from './services/swagger-fetch-clients';
import {AnalyticsTracker, setLoggedInState} from './utils/analytics';

declare const gapi: any;

const LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN = 'test-access-token-override';

const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
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

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = ({onSignIn, signIn, subscribeToInactivitySignOut, signOut}) => {
  const {authLoaded = false} = useStore(authStore);
  const isUserDisabled = useIsUserDisabled();

  const subscribeToAuth2User = (): void => {
    // The listen below only fires on changes, so we need an initial
    // check to handle the case where the user is already signed in.
    // authStore.set({...authStore.get(), authLoaded: true, isSignedIn: gapi.auth2.getAuthInstance().isSignedIn.get()});
    // this.zone.run(() => {
    //   const isSignedIn = gapi.auth2.getAuthInstance().isSignedIn.get();
    //   this.isSignedIn.next(isSignedIn);
    //   if (isSignedIn) {
    //     this.nextSignInStore();
    //     this.clearIdToken();
    //   }
    //   setLoggedInState(isSignedIn);
    // });
    // gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
    //   authStore.set({...authStore.get(), isSignedIn});
    //   this.zone.run(() => {
    //     this.isSignedIn.next(isSignedIn);
    //     if (isSignedIn) {
    //       this.nextSignInStore();
    //       this.clearIdToken();
    //     }
    //   });
    // });


    const isSignedIn = gapi.auth2.getAuthInstance().isSignedIn.get();
    authStore.set({...authStore.get(), authLoaded: true, isSignedIn: isSignedIn});
    setLoggedInState(isSignedIn);
    console.log("SubscribeToAuth2User: ", isSignedIn);

    const conf = new Configuration({
      basePath: getApiBaseUrl(),
      accessToken: () => currentAccessToken()
    });

    bindApiClients(conf);
    notebooksBindApiClients(conf);

    // TODO - this.nextSignInStore() logic
    // TODO - this.clerIdToken() logic
    // TODO - stuff about isSignedIn subscribable

    // TODO - for subscribing
    // gapi.auth2.getAuthInstance().isSignedIn.listen((isSignedIn: boolean) => {
    //   authStore.set({...authStore.get(), isSignedIn});
    //   this.zone.run(() => {
    //     this.isSignedIn.next(isSignedIn);
    //     if (isSignedIn) {
    //       this.nextSignInStore();
    //       this.clearIdToken();
    //     }
    //   });
    // });
  };

  const makeAuth2 = (config: ConfigResponse): Promise<any> => {
    console.log("makeAuth2: ", config);

    return new Promise((resolve) => {
      gapi.load('auth2', () => {
        gapi.auth2.init({
          client_id: environment.clientId,
          hosted_domain: config.gsuiteDomain,
          scope: 'https://www.googleapis.com/auth/plus.login openid profile'
            + (config.enableBillingUpgrade ? ' https://www.googleapis.com/auth/cloud-billing' : '')
        }).then(() => {
          subscribeToAuth2User();
        });
        resolve(gapi.auth2);
      });
    });
  }

  const serverConfigStoreCallback = (config: ConfigResponse) => {
    console.log("serverConfigStoreCallback: ", config);

    // TODO angular2react - restore puppeteer behavior
    // Enable test access token override via local storage. Intended to support
    // Puppeteer testing flows. This is handled in the server config callback
    // for signin timing consistency. Normally we cannot sign in until we've
    // loaded the oauth client ID from the config service.
    // if (environment.allowTestAccessTokenOverride) {
    //   this.testAccessTokenOverride = window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
    //   if (this.testAccessTokenOverride) {
    //     console.log('found test access token in local storage, skipping normal auth flow');
    //
    //     // The client has already configured an access token override. Skip the normal oauth flow.
    //     authStore.set({...authStore.get(), authLoaded: true, isSignedIn: true});
    //     this.zone.run(() => {
    //       this.isSignedIn.next(true);
    //     });
    //     return;
    //   }
    // }

    makeAuth2(config);
  };

  const loadConfig = async() => {
    const config = await configApi().getConfig();
    serverConfigStore.set({config: config});
  };

  useEffect(() => {
    loadConfig();
  }, []);

  useEffect(() => {
    if (serverConfigStore.get().config) {
      serverConfigStoreCallback(serverConfigStore.get().config);
    } else {
      const {unsubscribe} = serverConfigStore.subscribe((configStore) => {
        unsubscribe();
        serverConfigStoreCallback(configStore.config);
      });
    }
  }, []);

  const currentAccessToken = () => {
    // TODO angular2react - testAccess case
    const testAccessTokenOverride = null;
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

  console.log("Rendering AppRouting: ", authLoaded, isUserDisabled);

  return authLoaded && isUserDisabled !== undefined && <React.Fragment>
    {/* Once Angular is removed the app structure will change and we can put this in a more appropriate place */}
    <NotificationModal/>
    <AppRouter>
      {/* Previously, using a top-level Switch with AppRoute and ProtectedRoute has caused bugs: */}
      {/* see https://github.com/all-of-us/workbench/pull/3917 for details. */}
      {/* It should be noted that the reason this is currently working is because Switch only */}
      {/* duck-types its children; it cares about them having a 'path' prop but doesn't validate */}
      {/* that they are a Route or a subclass of Route. */}
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
                  subscribeToInactivitySignOut={() => {}} // Add subscription to sign out maybe?
                  signOut={signOut}
              />}
          />
        </ProtectedRoutes>
      </Switch>
    </AppRouter>
  </React.Fragment>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['onSignIn', 'signIn', 'subscribeToInactivitySignOut', 'signOut']);
    this.onSignIn = this.onSignIn.bind(this);
    this.signIn = this.signIn.bind(this);
    this.subscribeToInactivitySignOut = this.subscribeToInactivitySignOut.bind(this);
    this.signOut = this.signOut.bind(this);
  }

  onSignIn(): void {
    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        return <Redirect to='/'/>;
      }
    });
  }

  signIn(): void {
    AnalyticsTracker.Registration.SignIn();
    this.signInService.signIn();
  }

  subscribeToInactivitySignOut(): Subscription {
    return this.signInService.isSignedIn$.subscribe(signedIn => {
      if (!signedIn) {
        this.signOut();
      }
    });
  }

  signOut(): void {
    this.signInService.signOut();
  }
}
