import {HelpSidebar} from 'app/components/help-sidebar';
import {Spinner} from 'app/components/spinners';
import {WorkspaceNavBar} from 'app/pages/workspace/workspace-nav-bar';
import {WorkspaceRoutes} from 'app/routing/workspace-app-routing';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {reportError} from 'app/utils/errors';
import {withCurrentWorkspace} from 'app/utils';
import {
  ExceededActionCountError,
  LeoRuntimeInitializationAbortedError,
  LeoRuntimeInitializer
} from 'app/utils/leo-runtime-initializer';
import {currentWorkspaceStore, nextWorkspaceWarmupStore} from 'app/utils/navigation';
import {diskStore, MatchParams, routeDataStore, runtimeStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';
import { useParams } from 'react-router-dom';

export const WorkspaceWrapper = fp.flow(
  withCurrentWorkspace()
)(({workspace, hideSpinner}) => {
  useEffect(() => hideSpinner(), []);
  const routeData = useStore(routeDataStore);

  const [pollAborter, setPollAborter] = useState(new AbortController());
  const params = useParams<MatchParams>();
  const {ns, wsid} = params;

  useEffect(() => {
    const updateStores = async(namespace) => {
      diskStore.set({workspaceNamespace: namespace, persistentDisk: undefined});
      runtimeStore.set({workspaceNamespace: namespace, runtime: undefined, runtimeLoaded: false});
      pollAborter.abort();
      const newPollAborter = new AbortController();
      setPollAborter(newPollAborter);

      try {
        await LeoRuntimeInitializer.initialize({
          workspaceNamespace: namespace,
          pollAbortSignal: newPollAborter.signal,
          maxCreateCount: 0,
          maxResumeCount: 0
        });
      } catch (e) {
        // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
        // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
        // initialization here.
        // Also ignore LeoRuntimeInitializationAbortedError - this is expected when navigating
        // away from a page during a poll.
        if (!(e instanceof ExceededActionCountError || e instanceof LeoRuntimeInitializationAbortedError)) {
          // Ideally, we would have some top-level error messaginag here.
          reportError(e);
        }
      }
    };

    const getWorkspaceAndUpdateStores = async(namespace, id) => {
      // No destructuring because otherwise it shadows the workspace in props
      const wsResponse = await workspacesApi().getWorkspace(namespace, id);
      currentWorkspaceStore.next({
        ...wsResponse.workspace,
        accessLevel: wsResponse.accessLevel
      });

      updateStores(wsResponse.workspace.namespace);
    };

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
        updateStores(ns);
      } else {
        getWorkspaceAndUpdateStores(ns, wsid);
      }
    }
  }, [ns, wsid]);

  useEffect(() => {
    workspacesApi().updateRecentWorkspaces(ns, wsid);
  }, [ns, wsid]);

  return <React.Fragment>
    {workspace
        ? <React.Fragment>
          {!routeData.minimizeChrome && <WorkspaceNavBar tabPath={routeData.workspaceNavBarTab}/>}
          <HelpSidebar pageKey={routeData.pageKey}/>
          <div style={{marginRight: '45px', height: !routeData.contentFullHeightOverride ? 'auto' : '100%'}}>
            <WorkspaceRoutes/>
          </div>
        </React.Fragment>
        : <div style={{display: 'flex', height: '100%', width: '100%', justifyContent: 'center', alignItems: 'center'}}>
          <Spinner />
        </div>
    }
  </React.Fragment>;
});
