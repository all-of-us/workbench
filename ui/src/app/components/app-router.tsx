import * as React from 'react';
import { useEffect } from 'react';
import * as ReactDOM from 'react-dom';
import {
  BrowserRouter,
  Link,
  Redirect,
  Route,
  useLocation,
  useParams,
  useRouteMatch,
} from 'react-router-dom';
import * as fp from 'lodash/fp';

import { cond } from '@terra-ui-packages/core-utils';
import { routeDataStore } from 'app/utils/stores';
import { buildPageTitleForEnvironment } from 'app/utils/title';

import { Button } from './buttons';
import { Modal, ModalBody, ModalFooter, ModalTitle } from './modals';

export interface Guard {
  allowed: () => boolean;
  // One of redirectPath or renderBlocked should be specified
  redirectPath?: string;
  renderBlocked?: () => React.ReactElement;
}

export const usePath = () => {
  const { path } = useRouteMatch();
  return path;
};

export const parseQueryParams = (search: string) => {
  return new URLSearchParams(search);
};

/**
 * Retrieve query parameters from the React Router.
 *
 * Example:
 *  my/query/page?user=alice123
 *  reactRouterUrlSearchParams.get('user') -> value is 'alice123'
 */
export const useQuery = (): URLSearchParams => {
  const location = useLocation();
  return parseQueryParams(location.search);
};

export const withRouteData =
  (WrappedComponent) =>
  ({ intermediaryRoute = false, routeData, ...props }) => {
    const params = useParams();

    useEffect(() => {
      if (!intermediaryRoute) {
        if (!fp.isEqual(routeDataStore.get(), routeData)) {
          routeDataStore.set(routeData);
        }

        document.title = buildPageTitleForEnvironment(
          routeData.title || params[routeData.pathElementForTitle]
        );
      }
    }, [routeData]);

    return <WrappedComponent {...props} />;
  };

export const withFullHeight =
  (WrappedComponent) =>
  ({ ...props }) => {
    return (
      <div style={{ height: '100%' }}>
        <WrappedComponent {...props} />
      </div>
    );
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
      <ModalBody>{message}</ModalBody>
      <ModalFooter>
        <Button type='link' onClick={() => withCleanup(false)}>
          Cancel
        </Button>
        <Button type='primary' onClick={() => withCleanup(true)}>
          Discard Changes
        </Button>
      </ModalFooter>
    </Modal>,
    modal
  );
};

export const AppRouter = ({ children }): React.ReactElement => {
  return (
    <BrowserRouter getUserConfirmation={getUserConfirmation}>
      {children}
    </BrowserRouter>
  );
};

// Most internal routing is done via custom styled Button, not via text, so we only want to use anchor styling
// if we explicitly set it on the RouteLink
export const RouteLink = ({
  path,
  style = {},
  disabled = false,
  children,
}): React.ReactElement => {
  const linkStyles = { textDecoration: 'none', color: 'unset' };
  return disabled ? (
    <span style={{ ...linkStyles, ...style, cursor: 'not-allowed' }}>
      {children}
    </span>
  ) : (
    <Link style={{ ...linkStyles, ...style }} to={path}>
      {children}
    </Link>
  );
};

export const AppRoute = ({
  path,
  guards = [],
  exact,
  children,
}): React.ReactElement => {
  const { renderBlocked = null, redirectPath = null } =
    fp.find(({ allowed }) => !allowed(), guards) || {};

  return (
    <Route exact={exact} path={path}>
      {cond(
        [!!redirectPath, () => <Redirect to={redirectPath} />] as [
          boolean,
          () => React.ReactNode
        ],
        [!!renderBlocked, () => renderBlocked()] as [
          boolean,
          () => React.ReactNode
        ],
        () => children
      )}
    </Route>
  );
};

export const Navigate = ({ to }): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{ pathname: to, state: { from: location } }} />;
};
