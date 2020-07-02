import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, ProtectedRoutes, withFullHeight, withRouteData} from 'app/components/app-router';
import {CookiePolicy} from 'app/pages/cookie-policy';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import {SessionExpired} from 'app/pages/session-expired';
import {SignInAgain} from 'app/pages/sign-in-again';
import {UserDisabled} from 'app/pages/user-disabled';
import {SignInService} from 'app/services/sign-in.service';
import { ReactWrapperBase } from 'app/utils';
import {authStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';

const signInGuard: Guard = {
  allowed: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const DUCC = fp.flow(withRouteData, withFullHeight)(DataUserCodeOfConduct);

export const AppRoutingComponent: React.FunctionComponent<{signIn: Function}> = ({signIn}) => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicy}/>
    <AppRoute path='/session-expired' data={{signIn: signIn}} component={SessionExpired}/>
    <AppRoute path='/sign-in-again' data={{signIn: signIn}} component={SignInAgain}/>
    <AppRoute path='/user-disabled' component={UserDisabled}/>

    <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
        path='/data-code-of-conduct'
          component={ () => <DUCC routeData={{
            title: 'Data User Code of Conduct',
            minimizeChrome: true
          }} />}
        />
    </ProtectedRoutes>
  </AppRouter>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(AppRoutingComponent, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
