import * as React from 'react';
import { useEffect } from 'react';
import Iframe from 'react-iframe';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { parseQueryParams } from 'app/components/app-router';
import { findApp, UIAppType } from 'app/components/apps-panel/utils';
import { FlexRow } from 'app/components/flex';
import { WarningIcon } from 'app/components/icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { GKE_APP_PROXY_PATH_SUFFIX } from 'app/utils/constants';
import {
  MatchParams,
  UserAppsStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  userAppsStore: UserAppsStore;
}

export const GKEAppLauncher = fp.flow(withRouter)((props: Props) => {
  useEffect(() => {
    props.hideSpinner();
  }, []);
  const { userApps } = useStore(userAppsStore);

  const queryParams = parseQueryParams(props.location.search);
  const appType = queryParams.get('appType') as UIAppType;
  const userApp = findApp(userApps, appType);
  if (!!userApp) {
    const url = userApp.proxyUrls[GKE_APP_PROXY_PATH_SUFFIX];
    return (
      <div style={{ height: '100%' }}>
        <div style={{ borderBottom: '5px solid #2691D0', width: '100%' }} />
        <Iframe
          title='Gke-App embed'
          frameBorder={0}
          url={url}
          width='100%'
          height='100%'
        />
      </div>
    );
  } else {
    return (
      <FlexRow style={{ paddingTop: '5rem', paddingLeft: '5rem' }}>
        <WarningIcon />
        <label>Something went wrong please try later</label>
      </FlexRow>
    );
  }
});
