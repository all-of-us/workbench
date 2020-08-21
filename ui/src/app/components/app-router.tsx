import {navigate} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import { BrowserRouter, Link, Redirect, Route, Switch, useHistory, useLocation, useParams, useRouteMatch} from 'react-router-dom';

const {Fragment} = React;

export interface Guard {
  allowed: () => boolean;
  redirectPath: string;
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

// To compensate for Angular, while keeping true to the declarative/componentized nature of the router
// We will utilize a redirect component that uses the Angular navigation.
// Upon completing the migration this can be replaced with a react-router Redirect component
const NavRedirect = ({path}) => {
  navigate([path]);
  return null;
};

// A Switch (which, ultimately, is the innermost element in an AppRouter) expects its
// immediate children to be Routes. If they're not, the code will _probably_ still work, but
// may behave unexpectedly if the child component doesn't have all the props that a Route should.
// Duck typing is bad, so we use a function to build the Routes in-place.
export function generateRoute({path, data={}, guards=[], component: Component}): React.ReactElement {
  const {redirectPath = null} = fp.find(({allowed}) => !allowed(), guards) || {};

  return redirectPath
      ? <NavRedirect path={redirectPath}/>
      : <Route exact={true} path={path} >
    <Component urlParams={() => useParams()} routeHistory={() => useHistory()} routeConfig={data}/>
  </Route>;
}

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};

