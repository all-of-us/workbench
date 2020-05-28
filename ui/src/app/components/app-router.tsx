import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Link, Redirect, Route, Switch, useHistory, useLocation, useParams, useRouteMatch} from 'react-router-dom';

const {Fragment} = React;

export const usePath = () => {
  const {path} = useRouteMatch();
  return path;
};

export const SubRoute = ({children}) => <Switch>{children}</Switch>;
export const AppRouter = ({children}) => <BrowserRouter><SubRoute>{children}</SubRoute></BrowserRouter>;

export const RouteLink = ({path, style = {}, children}) => <Link style={{...style}} to={path}>{children}</Link>;

export const AppRoute = ({path, data = {}, component: Component}) => {
  const routeParams = useParams();
  const routeHistory = useHistory();

  return <Route path={path} >
    <Component urlParams={routeParams} routeHistory={routeHistory} routeConfig={data}/>
  </Route>;
};

export const ProtectedRoutes = ({path, guards, children}) => {
  const {redirectPath} = fp.find(({needsRedirect}) => needsRedirect(), guards) || {};
  const location = useLocation();
  return redirectPath ? <AppRoute
      path={path}
      component={() => <Redirect to={{pathname: redirectPath, state: {from: location} }}/>}/>
  : <Fragment>{children}</Fragment>;
};

export const Navigate = ({to}) => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
