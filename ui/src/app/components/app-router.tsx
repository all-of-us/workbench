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
import { BrowserRouter, Link, Redirect, Route, Switch, useLocation, useParams, useRouteMatch} from 'react-router-dom';
import {Button} from './buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';
import * as ReactDOM from 'react-dom';

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

  console.log(routeData);

  useEffect(() => {
    if (!intermediaryRoute) {
      routeConfigDataStore.next(routeData);

      if (!fp.isEqual(routeDataStore.get(), routeData)) {
        routeDataStore.set(routeData);
      }
    }
  }, [routeData]);

  useEffect(() => {
    if (intermediaryRoute) {
      // TODO angular2react: this is also pretty hacky but here goes
      // 1. WorkspaceWrapper needs the <WorkspaceWrapperPage> to set the workspace namespace and ID in order to render
      // 2. Flipping `intermediaryRoute` to false will fix that issue but another one surfaces
      // 3. If the intermediary route (WorkspaceWrapper) AND the final route both set values to the store, the values
      // in the intermediary route will overwrite the ones from the final route because of the order of resolving
      // useEffect (children first)
      //
      // As a result, urlParam values specified in the final route will be lost from the store. This applies to any
      // page within WorkspaceWrapper that specifies additional urlParam values that are not just `ns` and `wsid` which
      // are defined in the WorkspaceWrapper route. One specific example is the `nbName` parameter in read only notebooks.
      //
      // As a workaround, I changed the behavior of `intermediaryRoute` to only upsert its values instad of overwriting.
      // This does fix the issue but the definition and behavior of `intermediaryRoute` is getting a bit hard to understand
      // so we should revisit.
      urlParamsStore.next({...urlParamsStore.getValue(), ...params});
    } else {
      console.log(params);
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

const getUserConfirmation = (message, callback) => {
  const modal = document.createElement('div')
  document.body.appendChild(modal)

  const withCleanup = (answer) => {
    ReactDOM.unmountComponentAtNode(modal)
    document.body.removeChild(modal)
    callback(answer)
  };

  ReactDOM.render(
    <Modal>
      <ModalTitle>Warning!</ModalTitle>
      <ModalBody>
        {message}
      </ModalBody>
      <ModalFooter>
        <Button type='link' onClick={() => withCleanup(false)}>Cancel</Button>
        <Button type='primary' onClick={() => withCleanup(true)}>Discard Changes</Button>
      </ModalFooter>
    </Modal>, modal);
}

export const SubRoute = ({children}): React.ReactElement => <Switch>{children}</Switch>;
export const AppRouter = ({children}): React.ReactElement => <BrowserRouter getUserConfirmation={getUserConfirmation}>{children}</BrowserRouter>;

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

export const AppRoute = ({path, guards = [], exact, children}): React.ReactElement => {
  const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};

  return <Route exact={exact} path={path}>
    {redirectPath
        ? <Redirect to={redirectPath}/>
        : (children)
    }
  </Route>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};
