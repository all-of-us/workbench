import {
  queryParamsStore,
  routeConfigDataStore,
  urlParamsStore
} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as querystring from 'querystring';
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

const useQuery = () => {
  const searchString = useLocation().search.replace(/^\?/, '');
  return querystring.parse(searchString);
};

// TODO angular2react: This isn't really the right place to be making the store updates but it's the
// best place I found while we're using both angular and react routers
export const withRouteData = WrappedComponent => ({intermediaryRoute = false, routeData, ...props}) => {
  const params = useParams();
  const query = useQuery();

  useEffect(() => {
    if (!intermediaryRoute) {
      routeConfigDataStore.next(routeData);

      if (!fp.isEqual(routeDataStore.get(), routeData)) {
        routeDataStore.set(routeData);
      }
    }
  }, [routeData]);

  useEffect(() => {
    if (!intermediaryRoute) {
      // console.log(params);
      urlParamsStore.next(params);
    }
  }, [params]);

  useEffect(() => {
    if (!intermediaryRoute) {
      // console.log(query);
      queryParamsStore.next(query);
    }
  }, [query]);

  return <WrappedComponent {...props}/>;
};

export const withFullHeight = WrappedComponent => ({...props}) => {
  return <div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter>{children}</BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

export const AppRoute = ({path, data = {}, guards = [], component: Component, exact = true}): React.ReactElement => {
  const routeParams = useParams();
  const routeHistory = useHistory();

  return <Route exact={exact} path={path} render={
    () => {
      const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};
      return redirectPath
        ? <Redirect to={redirectPath}/>
        : <Component urlParams={routeParams} routeHistory={routeHistory} routeConfig={data}/>;
    }}>
  </Route>;
};

export const GuardedRoute = ({path, guards = [], exact=true, children}): React.ReactElement => {
  const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};

  return <Route exact={exact} path={path}>
    {redirectPath
        ? <Redirect to={redirectPath}/>
        : (children)
    }
  </Route>
}

export const ProtectedRoutes = (
  {guards, children}: {guards: Guard[], children: React.ReactElement | React.ReactElement[] }): React.ReactElement => {
  console.log('Rendering Protected Routes');

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
