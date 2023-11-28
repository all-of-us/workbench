import * as React from 'react';
import { useEffect } from 'react';
import Iframe from 'react-iframe';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { findApp, UIAppType } from 'app/components/apps-panel/utils';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import { withUserAppsStore } from 'app/utils/runtime-utils';
import { MatchParams, UserAppsStore } from 'app/utils/stores';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  userAppsStore: UserAppsStore;
}

export const GKEAppLauncher = fp.flow(
  withRouter,
  withUserAppsStore()
)((props: Props) => {
  useEffect(() => {
    props.hideSpinner();
  }, []);
  const { userApps } = props.userAppsStore;

  const urlPathSplit = props.match.url.split('/');
  const uiAppType: UIAppType = urlPathSplit[4] as UIAppType;
  const userApp = findApp(userApps, uiAppType);
  const url = userApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX];
  return (
    <div style={{ height: '100%' }}>
      <div style={{ borderBottom: '5px solid #2691D0', width: '100%' }} />
      <Iframe
        title='Gke-app embed'
        frameBorder={0}
        url={url}
        width='100%'
        height='100%'
      />
    </div>
  );
});
