import {
  currentWorkspaceStore, nextWorkspaceWarmupStore,
  queryParamsStore,
  routeConfigDataStore,
  urlParamsStore
} from 'app/utils/navigation';
import {routeDataStore, runtimeStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as querystring from 'querystring';
import * as React from 'react';
import {useContext, useEffect, useState} from 'react';
import * as ReactDOM from 'react-dom';
import { BrowserRouter, Link, Redirect, Route, Switch, useLocation, useParams, useRouteMatch} from 'react-router-dom';
import {Button} from './buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from './modals';
import {workspacesApi} from "../services/swagger-fetch-clients";
import {ExceededActionCountError, LeoRuntimeInitializer} from "app/utils/leo-runtime-initializer";
import {buildPageTitleForEnvironment} from "app/utils/title";
import {
  ParamsContext,
  ParamsContextProvider
} from "app/routing/params-context-provider";

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

export const RouteLink = ({path, style = {}, children}): React.ReactElement => <Link style={{...style}} to={path}>{children}</Link>;

const ParamsContextShim = ({intermediaryRoute, children}) => {
  const newParams = useParams();
  const {params, setParams} = useContext(ParamsContext);
  useEffect(() => {
    if (!fp.isEqual(params, newParams) && !intermediaryRoute) {
      setParams(newParams);
    }
  }, [newParams]);

  return children;
}

export const AppRoute = ({path, guards = [], exact, intermediaryRoute = false, children}): React.ReactElement => {
  const { redirectPath = null } = fp.find(({allowed}) => !allowed(), guards) || {};

  return <Route exact={exact} path={path}>
    {redirectPath
        ? <Redirect to={redirectPath}/>
        : <ParamsContextShim intermediaryRoute={intermediaryRoute}>
          {children}
        </ParamsContextShim>
    }
  </Route>;
};

export const Navigate = ({to}): React.ReactElement => {
  const location = useLocation();
  return <Redirect to={{pathname: to, state: {from: location}}}/>;
};

export const AppRoutingWrapper = ({children}) => {
  const [pollAborter, setPollAborter] = useState(new AbortController());

  useEffect(() => {
    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    document.title = buildPageTitleForEnvironment();
    routeDataStore.subscribe(({title, pathElementForTitle}) => {
      document.title = buildPageTitleForEnvironment(title || urlParamsStore.getValue()[pathElementForTitle]);
    });
  }, []);

  useEffect(() => {
    const sub = urlParamsStore
    .map(({ns, wsid}) => ({ns, wsid}))
    .distinctUntilChanged(fp.isEqual)
    .switchMap(async({ns, wsid}) => {
      currentWorkspaceStore.next(null);
      // This needs to happen for testing because we seed the urlParamsStore with {}.
      // Otherwise it tries to make an api call with undefined, because the component
      // initializes before we have access to the route.
      if (!ns || !wsid) {
        return null;
      }

      // In a handful of situations - namely on workspace creation/clone,
      // the application will preload the next workspace to avoid a redundant
      // refetch here.
      const nextWs = nextWorkspaceWarmupStore.getValue();
      nextWorkspaceWarmupStore.next(undefined);
      if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
        return nextWs;
      }

      // TODO angular2react : do we really need this hack?
      // Hack to ensure auth is loaded before a workspaces API call.
      // await this.signInService.isSignedIn$.first().toPromise();

      return await workspacesApi().getWorkspace(ns, wsid).then((wsResponse) => {
        return {
          ...wsResponse.workspace,
          accessLevel: wsResponse.accessLevel
        };
      });
    })
    .subscribe(async(workspace) => {
      if (workspace === null) {
        // This handles the empty urlParamsStore story.
        return;
      }
      currentWorkspaceStore.next(workspace);
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
    });

    return sub.unsubscribe;
  }, []);

  useEffect(() => {
    return urlParamsStore
    .map(({ns, wsid}) => ({ns, wsid}))
    .debounceTime(1000) // Kind of hacky but this prevents multiple update requests going out simultaneously
    // due to urlParamsStore being updated multiple times while rendering a route.
    // What we really want to subscribe to here is an event that triggers on navigation start or end
    // Debounce 1000 (ms) will throttle the output events to once a second which should be OK for real life usage
    // since multiple update recent workspace requests (from the same page) within the span of 1 second should
    // almost always be for the same workspace and extremely rarely for different workspaces
    .subscribe(({ns, wsid}) => {
      if (ns && wsid) {
        workspacesApi().updateRecentWorkspaces(ns, wsid);
      }
    }).unsubscribe;
    // const {ns, wsid} = params;
    // debugger;
    // if (ns && wsid) {
    //   workspacesApi().updateRecentWorkspaces(ns, wsid);
    // }
  }, []);

  return <React.Fragment>
    {children}
  </React.Fragment>

}
