import {HelpSidebar} from 'app/components/help-sidebar';
import {Spinner} from 'app/components/spinners';
import {WorkspaceNavBar} from 'app/pages/workspace/workspace-nav-bar';
import {WorkspaceRoutes} from 'app/routing/workspace-app-routing';
import {withCurrentWorkspace} from 'app/utils';
import {routeDataStore, useStore} from 'app/utils/stores';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';

export const WorkspaceWrapper = fp.flow(
  withCurrentWorkspace()
)(({workspace, routeConfigData, hideSpinner}) => {
  useEffect(() => hideSpinner(), []);
  const routeData = useStore(routeDataStore);

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
