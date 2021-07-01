import * as fp from 'lodash/fp';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {useParams} from 'react-router-dom';
import {HelpSidebar} from '../../components/help-sidebar';
import {workspacesApi} from '../../services/swagger-fetch-clients';
import {withRouteConfigData} from '../../utils';
import {currentWorkspaceStore} from '../../utils/navigation';
import {WorkspaceRoutes} from '../../workspace-app-routing';
import {WorkspaceNavBarReact} from './workspace-nav-bar';
import {Spinner} from "../../components/spinners";

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

//    return sub.unsubscribe;
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
  </React.Fragment>
});
