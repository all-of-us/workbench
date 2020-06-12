import {navigate} from 'app/utils/navigation';
import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Link, Redirect, Route, Switch, useHistory, useLocation, useParams, useRouteMatch} from 'react-router-dom';

const {Fragment} = React;

export interface Guard {
  checkGuard: () => boolean;
  redirectPath: string;
}

export const usePath = () => {
  const {path} = useRouteMatch();
  return path;
};

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter><SubRoute>{children}</SubRoute></BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

// To compensate for Angular, while keeping true to the declarative/compnentized nature of the router
// We will utilize a redirect component that uses the Angular navigation.
// Upon completing the migration this can be replaced with a react-router Redirect component
const NavRedirect = ({path}) => {
  navigate([path]);
  return <div/>;
};

export const AppRoute = ({path, data = {}, component: Component}): React.ReactElement => {
  const routeParams = useParams();
  const routeHistory = useHistory();

  return <Route path={path} >
    <Component urlParams={routeParams} routeHistory={routeHistory} routeConfig={data}/>
  </Route>;
};

export const ProtectedRoutes = (
  {guards, children}: {guards: Guard[], children: any }): React.ReactElement => {
  const { redirectPath = null } = fp.find(({checkGuard}) => !checkGuard(), guards) || {};

  return redirectPath ? <NavRedirect path={redirectPath}/>
  : <Fragment>{children}</Fragment>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
