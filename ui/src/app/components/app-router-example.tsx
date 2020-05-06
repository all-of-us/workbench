import {Component as AComponent} from '@angular/core';
import {BuildWorkspace} from 'app/pages/workspace/workspace-edit';
import {WorkspaceList } from 'app/pages/workspace/workspace-list';
import { ReactWrapperBase } from 'app/utils';
import * as React from 'react';
import {AppRoute, AppRouter, ProtectedRoutes, RouteLink, SubRoute, usePath} from './app-router';


const {Fragment} = React;

const authGuard = {
  needsRedirect: () => true,
  redirectPath: '/test'
};

const registeredGuard = {
  needsRedirect: () => true,
  redirectPath: '/test'
};

const SubComponent = () => {
  const path = usePath();
  return <Fragment>
    <AppRoute path='/workspaces/build' component={BuildWorkspace}/>
    <SubRoute>
      <ProtectedRoutes guards={[authGuard, registeredGuard]} path={`${path}`}>
        <AppRoute path={`${path}/sub1`} component={() => <div>Test 1</div>}/>
        <AppRoute path={`${path}/sub2`} component={() => <div>Test 2</div>}/>)}
      </ProtectedRoutes>
    </SubRoute>
  </Fragment>;
};

const RouteTest = ({urlParams}) => {
  const {value} = urlParams;
  return <Fragment>
    <RouteLink path='/test'>RouteLink To Test</RouteLink>
    <div>URL Parameter value: {value}</div>
  </Fragment>;
};

export const AppRouterEx: React.FunctionComponent<any> = () =>
  <AppRouter>
    <AppRoute path='/route/:value' component={RouteTest}></AppRoute>
    <AppRoute path='/test' component={() => {
      return <div style={{display: 'flex', flexDirection: 'column'}}>
          <RouteLink path='/workspaces'>React Workspace</RouteLink>
          <RouteLink path='/route/value'>Route </RouteLink>
          <RouteLink path='/comp'>Sub Components</RouteLink>
          <div>Hello</div>
        </div>;
    }
    }/>
    <AppRoute path='/comp' component={SubComponent}/>
    <AppRoute path='/workspaces/build' component={BuildWorkspace}/>
    <AppRoute path='/workspaces' component = {WorkspaceList} data={{title:  'test'}}/>
  </AppRouter>;

@AComponent({
  template: '<div #root></div>'
})
export class AppRouterComponent extends ReactWrapperBase {
  constructor() {
    super(AppRouterEx, []);
  }
}
