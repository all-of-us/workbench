import {HelpSidebar} from 'app/components/help-sidebar';
import {Spinner} from 'app/components/spinners';
import {WorkspaceNavBarReact} from 'app/pages/workspace/workspace-nav-bar';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {withRouteConfigData} from 'app/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceRoutes} from 'app/workspace-app-routing';
import * as fp from 'lodash/fp';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {useParams} from 'react-router-dom';

export const WorkspaceWrapper = fp.flow(
  withRouteConfigData()
)(({routeConfigData}) => {
  const {ns, wsid} = useParams();
  const [workspace, setWorkspace] = useState(currentWorkspaceStore.getValue());

  useEffect(() => {
    if (!workspace || workspace.namespace !== ns || workspace.id !== wsid) {
      workspacesApi().getWorkspace(ns, wsid)
        .then((wsResponse) => {
          currentWorkspaceStore.next({
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel
          });
        });
    }
  }, [ns, wsid]);

  useEffect(() => {
    const sub = currentWorkspaceStore.subscribe(storeWorkspace => {
      setWorkspace(storeWorkspace);
    });

    return sub.unsubscribe;
  }, []);

  return <React.Fragment>
    {workspace
        ? <React.Fragment>
          {!routeConfigData.minimizeChrome && <WorkspaceNavBarReact tabPath={routeConfigData.workspaceNavBarTab}/>}
          <HelpSidebar pageKey={routeConfigData.pageKey}/>
          <div style={{marginRight: '45px', height: !routeConfigData.contentFullHeightOverride ? 'auto' : '100%'}}>
              <WorkspaceRoutes/>
          </div>
        </React.Fragment>
        : <div style={{display: 'flex', height: '100%', width: '100%', justifyContent: 'center', alignItems: 'center'}}>
          <Spinner />
        </div>
    }
  </React.Fragment>;
});
