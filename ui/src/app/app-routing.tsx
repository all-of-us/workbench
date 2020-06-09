import {Component as AComponent} from '@angular/core';
import {AppRoute, AppRouter} from 'app/components/app-router';
import { ReactWrapperBase } from 'app/utils';
import * as React from 'react';
import {CookiePolicyComponent} from './pages/cookie-policy';

export const AppRoutingComponent: React.FunctionComponent = () => <AppRouter>
    <AppRoute path='/cookie-policy' component={CookiePolicyComponent}/>
  </AppRouter>;

@AComponent({
  template: '<div #root></div>'
})
export class AppRouting extends ReactWrapperBase {
  constructor() {
    super(AppRoutingComponent, []);
  }
}
