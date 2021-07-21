import {HelpSidebar} from 'app/components/help-sidebar';
import {Spinner} from 'app/components/spinners';
import {WorkspaceNavBarReact} from 'app/pages/workspace/workspace-nav-bar';
import {withCurrentWorkspace, withRouteConfigData} from 'app/utils';
import {WorkspaceRoutes} from 'app/workspace-app-routing';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect} from 'react';
import {routeDataStore, withStore} from '../../utils/stores';

export const WorkspaceWrapper = fp.flow(
  withCurrentWorkspace(),
//  withRouteConfigData(),
  withStore(routeDataStore, 'routeData')
)(({workspace, routeData, routeConfigData, hideSpinner}) => {
  console.log("Rendering WorkspaceWrapper ", routeData);
  useEffect(() => hideSpinner(), []);

  return <React.Fragment>
    {workspace
        ? <React.Fragment>
          {!routeData.minimizeChrome && <WorkspaceNavBarReact tabPath={routeData.workspaceNavBarTab}/>}
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
