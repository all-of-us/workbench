import {Component as AComponent} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from 'react-router';

import {AppRoute, AppRouter, Guard, ProtectedRoutes, withRouteData} from 'app/components/app-router';
import {NotificationModal} from 'app/components/modals';
import {withRoutingSpinner} from 'app/components/with-routing-spinner';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {SignIn} from 'app/pages/login/sign-in';
import {SessionExpired} from 'app/pages/session-expired';
import {SignedIn} from 'app/pages/signed-in/signed-in';
import {SignInAgain} from 'app/pages/sign-in-again';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import {ReactWrapperBase} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {authStore, useStore} from 'app/utils/stores';

const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const CookiePolicyPage = fp.flow(withRouteData, withRoutingSpinner)(CookiePolicy);
const SessionExpiredPage = fp.flow(withRouteData, withRoutingSpinner)(SessionExpired);
const SignedInPage = fp.flow(withRouteData, withRoutingSpinner)(SignedIn);
const SignInAgainPage = fp.flow(withRouteData, withRoutingSpinner)(SignInAgain);
const SignInPage = fp.flow(withRouteData, withRoutingSpinner)(SignIn);
const UserDisabledPage = fp.flow(withRouteData, withRoutingSpinner)(UserDisabled);

interface RoutingProps {
  onSignIn: () => void;
  signIn: () => void;
  onSignOut: () => void;
  signOut: () => void;
}

export const AppRoutingComponent: React.FunctionComponent<RoutingProps> = ({onSignIn, signIn, onSignOut, signOut}) => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <React.Fragment>
    {/* Once Angular is removed the app structure will change and we can put this in a more appropriate place */}
    <NotificationModal/>
    <AppRouter>
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
      <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
            path=''
            exact={false}
            component={() => <SignedInPage intermediaryRoute={true} routeData={{}} onSignOut={onSignOut} signOut={signOut}/>}
        />
      </ProtectedRoutes>
    </AppRouter>
  </React.Fragment>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['onSignIn', 'signIn', 'onSignOut', 'signOut']);
    this.onSignIn = this.onSignIn.bind(this);
    this.signIn = this.signIn.bind(this);
    this.onSignOut = this.onSignOut.bind(this);
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

  onSignOut(): void {
    this.signInService.isSignedIn$.subscribe(signedIn => {
      if (!signedIn) {
        this.signOut();
      }
    });
  }

  signOut(): void {
    this.signInService.signOut();
  }
}
