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

const SubComponent = () => {
  const path = usePath();
  return <Fragment>
    <RouteLink href={`${path}/sub1`}>Sub 1</RouteLink>
    <RouteLink href={`${path}/sub2`} >Sub 2 (Will Redirect)</RouteLink>
    <SubRoute>
      <AppRoute path={`${path}/sub1`} component={() => <div>Test 1</div>}/>
      <ProtectedRoutes guards={[authGuard]} path={`${path}/sub2`}>
        <AppRoute path={`${path}/sub2`} component={() => <div>Test 2</div>}/>)}
      </ProtectedRoutes>
    </SubRoute>
  </Fragment>;
};

const RouteTest = ({urlParams}) => {
  const {value} = urlParams;
  return <Fragment>
    <RouteLink href='/test'>RouteLink To Test</RouteLink>
    <div>URL Parameter value: {value}</div>
  </Fragment>;
};

export const AppRouterEx: React.FunctionComponent<any> = () =>
  <AppRouter>
    <AppRoute path='/route/:value' component={RouteTest}></AppRoute>
    <AppRoute path='/test' component={() => {
      return <div style={{display: 'flex', flexDirection: 'column'}}>
          <RouteLink href='/workspaces'>React Workspace</RouteLink>
          <RouteLink href='/route/value'>Route </RouteLink>
          <RouteLink href='/comp'>Sub Components</RouteLink>
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
