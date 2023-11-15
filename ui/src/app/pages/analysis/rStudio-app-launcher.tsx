import * as React from 'react';
import { useEffect } from 'react';
import Iframe from 'react-iframe';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  AppStatus,
  Profile,
  Runtime,
  UserAppEnvironment,
} from 'generated/fetch';

import { findApp, UIAppType } from 'app/components/apps-panel/utils';
import { rstudioConfigIconId } from 'app/components/help-sidebar-icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  LeoApplicationType,
  Progress,
} from 'app/pages/analysis/leonardo-app-launcher';
import colors from 'app/styles/colors';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import {
  NavigationProps,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import { withRuntimeStore, withUserAppsStore } from 'app/utils/runtime-utils';
import { MatchParams, RuntimeStore, UserAppsStore } from 'app/utils/stores';
import { openRStudio } from 'app/utils/user-apps-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  workspace: WorkspaceData;
  profileState: { profile: Profile; reload: Function; updateCache: Function };
  userAppsStore: UserAppsStore;
}
interface State {
  rStudioUrl: string;
}
const getContent = (
  userApp: UserAppEnvironment,
  workspaceNamespace: string
) => {
  if (userApp?.status === AppStatus.RUNNING) {
    const url = userApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX];
    openRStudio(workspaceNamespace, userApp);
    return (
      <div style={{ height: '100%' }}>
        <div style={{ borderBottom: '5px solid #2691D0', width: '100%' }} />
        <Iframe frameBorder={0} url={url} width='100%' height='100%' />
      </div>
    );
  } else {
    setTimeout(function () {
      setSidebarActiveIconStore.next(rstudioConfigIconId);
    }, 500);
    return (
      <div
        style={{
          fontWeight: 600,
          color: colors.primary,
          paddingTop: '1rem',
          paddingLeft: '5rem',
        }}
      >
        RStudio is not Running, please start RStudio and then refresh the page
      </div>
    );
  }
};
export const RStudioAppLauncher = fp.flow(
  withUserProfile(),
  withCurrentWorkspace(),
  withRuntimeStore(),
  withNavigation,
  withRouter,
  withUserAppsStore()
)((props: Props) => {
  const { userApps } = props.userAppsStore;
  const userApp = findApp(userApps, UIAppType.RSTUDIO);

  useEffect(() => {
    props.hideSpinner();
  }, []);

  return getContent(userApp, props.workspace.namespace);
});
