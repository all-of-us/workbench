import {navigate, routeConfigDataStore, urlParamsStore} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';
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

// TODO angular2react: This isn't really the right place to be making the store updates but it's the
// best place I found while we're using both angular and react routers
export const withRouteData = WrappedComponent => ({intermediaryRoute = false, routeData, ...props}) => {
  const params = useParams();

  useEffect(() => {
    if (!intermediaryRoute) {
      routeConfigDataStore.next(routeData);
      routeDataStore.set(routeData);
    }
  }, [routeData]);

  useEffect(() => {
    if (!intermediaryRoute) {
      urlParamsStore.next(params);
    }
  }, [params]);

  return <WrappedComponent {...props}/>;
};

export const withFullHeight = WrappedComponent => ({...props}) => {
  return <div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter>{children}</BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

// To compensate for Angular, while keeping true to the declarative/componentized nature of the router
// We will utilize a redirect component that uses the Angular navigation.
// Upon completing the migration this can be replaced with a react-router Redirect component.
// Exported for testing.
export const NavRedirect = ({path}) => {
  navigate([path]);
  return null;
};

export const AppRoute = ({path, data = {}, guards = [], component: Component, exact = true}): React.ReactElement => {
  const routeParams = useParams();
  const routeHistory = useHistory();

  return <Route exact={exact} path={path} render={
    () => {
      const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};
      return redirectPath
        ? <NavRedirect path={redirectPath}/>
        : <Component urlParams={routeParams} routeHistory={routeHistory} routeConfig={data}/>;
    }}>
  </Route>;
};

export const ProtectedRoutes = (
  {guards, children}: {guards: Guard[], children: React.ReactElement | React.ReactElement[] }): React.ReactElement => {

  // Pass the guards to the individual routes. Be sure not to overwrite any existing guards
  const guardedChildren = fp.flow(
    fp.flatten,
    fp.toPairs,
    fp.map(
      ([key, element]: [string, React.ReactElement]) => {
        const {guards: elementGuards = []} = element.props;
        return React.cloneElement(element, {key, guards: [...guards, ...elementGuards ]});
      }
    )
  )([children]); // Make sure children is an array - a single child will not be in an array

  return <Fragment>{guardedChildren}</Fragment>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
