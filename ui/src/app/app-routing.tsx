import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, ProtectedRoutes} from 'app/components/app-router';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import { ReactWrapperBase } from 'app/utils';
import {authStore, routeDataStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {CookiePolicyComponent} from './pages/cookie-policy';


const signInGuard: Guard = {
  checkGuard: (): boolean => authStore.get().isSignedIn,
  redirectPath: '/login'
};

const withTitle = WrappedComponent => ({title, ...props}) => {
  routeDataStore.set({title});
  return <WrappedComponent {...props}/>;
};

const withFullHeight = WrappedComponent => ({...props}) => {
  return < div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

const DUCC = fp.flow(withTitle, withFullHeight)(DataUserCodeOfConduct);

export const AppRoutingComponent: React.FunctionComponent = () => {
  const {authLoaded = false} = useStore(authStore);

  return authLoaded && <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicyComponent}/>
    <ProtectedRoutes guards={[signInGuard]}>
        <AppRoute
        path='/data-code-of-conduct'
        component={ () => <DUCC title='Data User Code of Conduct'/>}
        />
    </ProtectedRoutes>
  </AppRouter>;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor() {
    super(AppRoutingComponent, []);
  }
}
