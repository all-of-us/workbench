import {ParamsContext} from 'app/routing/params-context-provider';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {ExceededActionCountError, LeoRuntimeInitializer} from 'app/utils/leo-runtime-initializer';
import {
  currentWorkspaceStore, nextWorkspaceWarmupStore,
  queryParamsStore,
  routeConfigDataStore
} from 'app/utils/navigation';
import {routeDataStore, runtimeStore, useStore} from 'app/utils/stores';
import {buildPageTitleForEnvironment} from 'app/utils/title';
import * as fp from 'lodash/fp';
import * as querystring from 'querystring';
import * as React from 'react';
import {useContext, useEffect, useState} from 'react';
import { Link, Redirect, Route, useLocation, useParams, useRouteMatch} from 'react-router-dom';

export interface MatchParams {
  cid?: string;
  csid?: string;
  dataSetId?: string;
  domain?: string;
  institutionId?: string;
  nbName?: string;
  ns?: string;
  pid?: string;
  username?: string;
  usernameWithoutGsuiteDomain?: string;
  wsid?: string;
}


export interface Guard {
  allowed: () => boolean;
  redirectPath: string;
}

export const usePath = () => {
  const {path} = useRouteMatch();
  return path;
};

export const useQuery = () => {
  const searchString = useLocation().search.replace(/^\?/, '');
  return querystring.parse(searchString);
};

// TODO angular2react: This isn't really the right place to be making the store updates but it's the
// best place I found while we're using both angular and react routers
export const withRouteData = WrappedComponent => ({intermediaryRoute = false, routeData, ...props}) => {
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
      queryParamsStore.next(query);
    }
  }, [query]);

  return <WrappedComponent {...props}/>;
};

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

const ParamsContextSetter = ({intermediaryRoute, children}) => {
  const newParams = useParams();
  const {paramsContext: {params, setParams}} = useContext(ParamsContext);
  useEffect(() => {
    if (!fp.isEqual(params, newParams)) {
      if (intermediaryRoute) {
        setParams({...params, ...newParams});
      } else {
        setParams(newParams);
      }
    }
  }, [newParams]);

  return children;
};

export const AppRoute = ({path, guards = [], exact, intermediaryRoute = false, children}): React.ReactElement => {
  const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};

  return <Route exact={exact} path={path}>
    {redirectPath
        ? <Redirect to={redirectPath}/>
        : <ParamsContextSetter intermediaryRoute={intermediaryRoute}>
          {children}
        </ParamsContextSetter>
    }
  </Route>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};

export const AppRoutingWrapper = ({children}) => {
  const [pollAborter, setPollAborter] = useState(new AbortController());
  const {paramsContext: {params}} = useContext(ParamsContext);
  const {title, pathElementForTitle} = useStore(routeDataStore);

  useEffect(() => {
    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    document.title = buildPageTitleForEnvironment(title || params[pathElementForTitle] || '');
  }, [params, title, pathElementForTitle]);

  useEffect(() => {
    const getWorkspaceAndUpdateStores = async(namespace, id) => {
      const wsResponse = await workspacesApi().getWorkspace(namespace, id);
      const {workspace, accessLevel} = wsResponse;
      currentWorkspaceStore.next({
        ...workspace,
        accessLevel: accessLevel
      });

      runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});
      pollAborter.abort();
      const newPollAborter = new AbortController();
      setPollAborter(newPollAborter);

      try {
        await LeoRuntimeInitializer.initialize({
          workspaceNamespace: workspace.namespace,
          pollAbortSignal: newPollAborter.signal,
          maxCreateCount: 0,
          maxDeleteCount: 0,
          maxResumeCount: 0
        });
      } catch (e) {
        // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
        // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
        // initialization here.
        if (!(e instanceof ExceededActionCountError)) {
          throw e;
        }
      }
    };

    const {ns, wsid} = params;

    if (!ns || !wsid) {
      return;
    }

    if (
        !currentWorkspaceStore.getValue()
        || currentWorkspaceStore.getValue().namespace !== ns
        || currentWorkspaceStore.getValue().id !== wsid
    ) {
      currentWorkspaceStore.next(null);
      // In a handful of situations - namely on workspace creation/clone,
      // the application will preload the next workspace to avoid a redundant
      // refetch here.
      const nextWs = nextWorkspaceWarmupStore.getValue();
      nextWorkspaceWarmupStore.next(undefined);
      if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
        currentWorkspaceStore.next(nextWs);
        return;
      } else {
        getWorkspaceAndUpdateStores(ns, wsid);
      }
    }
  }, [params]);

  useEffect(() => {
    const {ns, wsid} = params;
    if (ns && wsid) {
      workspacesApi().updateRecentWorkspaces(ns, wsid);
    }
  }, [params]);

  return <React.Fragment>
    {children}
  </React.Fragment>;

};
