import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter, Guard, ProtectedRoutes, SubRoute} from 'app/components/app-router';
import {DataUserCodeOfConduct} from 'app/pages/profile/data-user-code-of-conduct';
import { ReactWrapperBase } from 'app/utils';
import {signInStore, userProfileStore} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {CookiePolicyComponent} from './pages/cookie-policy';

import {SignedInComponent} from './pages/signed-in/component';

const {useEffect, useState} = React;

const signInGuard: Guard = {
  checkGuard: (): boolean => signInStore.getValue().isSignedIn(),
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
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const waitForStore = async() => {
      await serverConfigStore.filter(v => !!v).first().toPromise();
      await signInStore.filter(v => !!v).first().toPromise();
      setLoaded(true);
    };

    waitForStore();
  }, []);

  return loaded && <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicyComponent}/>
    {/* <ProtectedRoutes path='' guards={[]}> */}
      {/* <SubRoute> */}

        <AppRoute path='/data-code-of-conduct' component={ () => <DUCC title='Data User Code of Conduct'/>}/>;

    {/* </SubRoute> */}
    {/* </ProtectedRoutes>; */}
  </AppRouter > ;
};

@AComponent({
  template: '<div #root style="display: inline;"></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor() {
    super(AppRoutingComponent, []);
  }
}
