import {Component as AComponent} from '@angular/core';
import {BuildWorkspace} from 'app/pages/workspace/workspace-edit';
import {WorkspaceList } from 'app/pages/workspace/workspace-list';
// import {SignInService} from 'app/services/sign-in.service';
import { ReactWrapperBase } from 'app/utils';
import * as fp from 'lodash/fp';
// import {ConfigResponse, ConfigService} from 'generated';
import * as React from 'react';
import { BrowserRouter, Link, Redirect, Route, Switch, useHistory, useLocation, useParams, useRouteMatch} from 'react-router-dom';

const {Fragment} = React;

const RouteTest = () => {
  const {value} = useParams();
  return <Fragment>
    <Link to='/test'>Link To Test</Link>
    <div>URL Parameter value: {value}</div>
  </Fragment>;
};

// const signInService = new ConfigService();
// console.log(SignInService);
// const AuthenticatedRoute = ({children, ...props}) => {
// };

const authGuard = {
  failedCheck: () => false,
  redirectPath: '/login'
};

const AppRoute = ({path, data, component: Component}) => {
  const routeParams = useParams();
  const routeHistory = useHistory();
  return <Route path={path} >
    <Component urlParams={routeParams} routeHistor={routeHistory} routeConfig={data}/>
  </Route>;
};

const PrivateRoute = ({path, guards, component, data}) => {
  const location = useLocation();

  const {redirectPath} = fp.find(({failedCheck}) => failedCheck(), guards) || {};
  return redirectPath ? <Redirect to={{pathname: redirectPath, state: {from: location} }}/>
    : <AppRoute path={path} component={component} data={data}/>;
};

// const NestedRoute = ({path, children});

const SubComponent = () => {
  const {path} = useRouteMatch();
  return <Fragment>
    <Link to={`${path}/sub1`}>Sub 1</Link>
    <Link to={`${path}/sub2`} > Sub 2</Link >
    <Switch>
      <AppRoute path={`${path}/sub1`} component={() => <div>Test 1</div>}/>
      <AppRoute path={`${path}/sub2`} component={() => <div>Test 2</div>}/>
    </Switch>;
  </Fragment > ;
};

const AppRouter: React.FunctionComponent<any> = () =>
  <BrowserRouter>
    <Switch>
      <AppRoute path='/route/:value' component={RouteTest}></AppRoute>
      <Route path='/test'>
        <div style={{display: 'flex', flexDirection: 'column'}}>
          <Link to='/workspaces'>React Workspace Link</Link>
          <Route path='/builder'/>
          <Link to='/route/value'>Route Link</Link>
          <Link to='/comp'>Sub Components</Link>
          <div>Hello</div>
        </div>
      </Route>
      <AppRoute path='/comp' component={SubComponent}/>
      <AppRoute guards={[authGuard]} path='/workspaces/build' component={BuildWorkspace}/>
      <AppRoute path='/workspaces' component = {WorkspaceList} data={{title:  'test'}}/>
    </Switch>
  </BrowserRouter>;

/////////////////////////////////////////////////////////////////////////////////////////////////////////

const AppRouter2: React.FunctionComponent<any> = () =>
  <BrowserRouter>
    <Switch>
      < AppRoute path='/route/:value'><RouteTest/></ AppRoute>
      < AppRoute path='/comp'><SubComponent/></ AppRoute>
      < AppRoute path='/workspaces'><WorkspaceList/></ AppRoute>
      <PrivateRoute guards={[authGuard]} path='/workspaces/build' component={BuildWorkspace}><BuildWorkspace/></PrivateRoute>
    </Switch>
  </BrowserRouter>;



const ProtectedRoutes = ({guards}) => {
  return <div>{guards}</div>;
};

const AppRouter3: React.FunctionComponent<any> = () =>
<BrowserRouter>
  <Switch>
    <AppRoute path='/route/:value' component={RouteTest}/>
    <AppRoute path='/comp' component={SubComponent}/>
    <AppRoute path='/workspaces' component={WorkspaceList}/>
    <ProtectedRoutes guards={[authGuard]}>
      <AppRoute  path='/workspaces/build' component={BuildWorkspace}/>
    </ProtectedRoutes>
  </Switch>
</BrowserRouter>;

@AComponent({
  template: '<div #root></div>'
})
export class AppRouterComponent extends ReactWrapperBase {
  constructor() {
    super(AppRouter, []);
  }
}

