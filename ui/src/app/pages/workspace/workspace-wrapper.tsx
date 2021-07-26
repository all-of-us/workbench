import {HelpSidebar} from 'app/components/help-sidebar';
import {Spinner} from 'app/components/spinners';
import {WorkspaceNavBar} from 'app/pages/workspace/workspace-nav-bar';
import {withCurrentWorkspace, withRouteConfigData} from 'app/utils';
import {WorkspaceRoutes} from 'app/workspace-app-routing';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';

export const WorkspaceWrapper = fp.flow(
  withCurrentWorkspace(),
  withRouteConfigData()
)(({workspace, routeConfigData, hideSpinner}) => {
  useEffect(() => hideSpinner(), []);
  console.log("Rendering WorkspaceWrapper", workspace);

  return <React.Fragment>
    {workspace
        ? <React.Fragment>
          {!routeConfigData.minimizeChrome && <WorkspaceNavBar tabPath={routeConfigData.workspaceNavBarTab}/>}
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
