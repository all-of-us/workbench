import * as React from 'react';
import { useEffect } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import {
  CompatRoute,
  CompatRouter,
  Navigate,
} from 'react-router-dom-v5-compat';
import * as fp from 'lodash/fp';

import { cond } from '@terra-ui-packages/core-utils';
import { routeDataStore } from 'app/utils/stores';
import { buildPageTitleForEnvironment } from 'app/utils/title';

export interface Guard {
  allowed: () => boolean;
  // One of redirectPath or renderBlocked should be specified
  redirectPath?: string;
  renderBlocked?: () => React.ReactElement;
}

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

export const AppRouter = ({ children }): React.ReactElement => (
  <CompatRouter>{children}</CompatRouter>
);

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
    <CompatRoute {...{ path, exact }}>
      {cond<React.ReactNode>(
        [redirectPath, () => <Navigate to={redirectPath} />],
        [renderBlocked, () => renderBlocked()],
        () => children
      )}
    </CompatRoute>
  );
};
