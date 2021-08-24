import {queryParamsStore, routeConfigDataStore} from 'app/utils/navigation';
import {routeDataStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as querystring from 'querystring';
import * as React from 'react';
import {useEffect} from 'react';
import * as ReactDOM from 'react-dom';
import { BrowserRouter, Link, Redirect, Route, useLocation, useRouteMatch, useParams} from 'react-router-dom';
import {Button} from './buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';
import {buildPageTitleForEnvironment} from "../utils/title";

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
  const query = useQuery();
  const params = useParams();

  useEffect(() => {
    if (!intermediaryRoute) {
      routeConfigDataStore.next(routeData);

      if (!fp.isEqual(routeDataStore.get(), routeData)) {
        routeDataStore.set(routeData);
      }
    }

    document.title = buildPageTitleForEnvironment(routeData.title || params[routeData.pathElementForTitle]);
  }, [routeData]);

  useEffect(() => {
    if (!intermediaryRoute) {
      queryParamsStore.next(query);
    }
  }, [query]);

  return <WrappedComponent {...props}/>;
};

export const withFullHeight = WrappedComponent => ({...props}) => {
  return <div style={{height: '100%'}}><WrappedComponent {...props} /></div>;
};

// This function is invoked if react-router `<Prompt>` is rendered by a component that wants the user to
// confirm navigating away from the page. The default behavior of <Prompt> is being overridden by this
// getUserConfirmation function so we can provide a custom styled warning modal instead of the browser's default.
const getUserConfirmation = (message, callback) => {
  const modal = document.createElement('div');
  document.body.appendChild(modal);

  const withCleanup = (answer) => {
    ReactDOM.unmountComponentAtNode(modal);
    document.body.removeChild(modal);
    callback(answer);
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
};

export const AppRouter = ({children}): React.ReactElement => {
  return <BrowserRouter getUserConfirmation={getUserConfirmation}>{children}</BrowserRouter>;
};

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

export const AppRoute = ({path, guards = [], exact, intermediaryRoute = false, children}): React.ReactElement => {
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
