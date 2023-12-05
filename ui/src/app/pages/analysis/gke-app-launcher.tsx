import * as React from 'react';
import { useEffect } from 'react';
import Iframe from 'react-iframe';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { AppStatus } from 'generated/fetch';

import { findApp, UIAppType } from 'app/components/apps-panel/utils';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  ErrorMode,
  NotebookFrameError,
} from 'app/pages/analysis/notebook-frame-error';
import { analysisTabPath } from 'app/routing/utils';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import { currentWorkspaceStore, useNavigation } from 'app/utils/navigation';
import { MatchParams, userAppsStore, useStore } from 'app/utils/stores';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

export const GKEAppLauncher = fp.flow(withRouter)((props: Props) => {
  const { userApps } = useStore(userAppsStore);
  const appType = props.match.params.appType as UIAppType;
  const userApp = findApp(userApps, appType);
  const [navigate] = useNavigation();

  useEffect(() => {
    props.hideSpinner();
    // In case app is deleted redirect user to the analysis tab.
    if (!!userApp && userApp.status === AppStatus.DELETING) {
      const { namespace, id } = currentWorkspaceStore.getValue();
      navigate([analysisTabPath(namespace, id)]);
    }
  }, [userApp]);

  if (!!userApp) {
    const url = userApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX];
    return (
      <div style={{ height: '100%' }}>
        <div style={{ borderBottom: '5px solid #2691D0', width: '100%' }} />
        <Iframe
          title={`${appType} embed`}
          frameBorder={0}
          url={url}
          width='100%'
          height='100%'
        />
      </div>
    );
  } else {
    return (
      <NotebookFrameError errorMode={ErrorMode.ERROR}>
        An error was encountered with your {appType} Environment. To resolve,
        please see the Applications side panel.
      </NotebookFrameError>
    );
  }
});
