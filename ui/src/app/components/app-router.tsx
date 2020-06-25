import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Link, Redirect, Route, Switch, useHistory, useLocation, useParams, useRouteMatch} from 'react-router-dom';

const {Fragment} = React;

interface Guards {
  [index: number]: {
    checkGuard: () => boolean;
    redirectPath: string;
  };
}

export const usePath = () => {
  const {path} = useRouteMatch();
  return path;
};

export const withRouteData = WrappedComponent => ({routeData, ...props}) => {
  routeDataStore.set(routeData);
  return <WrappedComponent {...props}/>;
};

export const withFullHeight = WrappedComponent => ({...props}) => {
  return <div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter><SubRoute>{children}</SubRoute></BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

export const AppRoute = ({path, data = {}, component: Component}): React.ReactElement => {
  const routeParams = useParams();
  const routeHistory = useHistory();

  return <Route path={path} >
    <Component urlParams={routeParams} routeHistory={routeHistory} routeConfig={data}/>
  </Route>;
};

export const ProtectedRoutes = (
  {path, guards, children}: {path: string, guards: Guards, children: React.ReactNode[] }): React.ReactElement => {
  const { redirectPath } = fp.find(({checkGuard}) => checkGuard(), guards);
  const location = useLocation();
  return redirectPath ? <AppRoute
      path={path}
      component={() => <Redirect to={{pathname: redirectPath, state: {from: location} }}/>}/>
  : <Fragment>{children}</Fragment>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
