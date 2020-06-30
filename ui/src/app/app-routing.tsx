import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter} from 'app/components/app-router';
import { ReactWrapperBase } from 'app/utils';
import * as React from 'react';
import {UserAudit} from './pages/admin/user-audit';
import {CookiePolicyComponent} from './pages/cookie-policy';

export const AppRoutingComponent: React.FunctionComponent = () => {
  return <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicyComponent}/>
    <AppRoute path='/user-audit' component={UserAudit}/>
  </AppRouter>;
};

@AComponent({
  template: '<div #root></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor() {
    super(AppRoutingComponent, []);
  }
}
