import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { environment } from 'environments/environment';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { ajaxContext, Environments } from 'terraui/out/Environments';

const leoUrlRoot = environment.leoApiUrl;

const ajax = (getAccessToken) => (signal) => {
  const fetchOpts = () => ({
    signal,
    headers: { authorization: 'bearer ' + getAccessToken() },
  });
  return {
    Workspaces: {
      list: () => workspacesApi().getWorkspaces(),
    },
    Runtimes: {
      listV2: () =>
        fetch(
          leoUrlRoot + '/api/v2/runtimes?includeDeleted=false',
          fetchOpts()
        ).then((r) => r.json()),
    },
    Apps: {
      listWithoutProject: () =>
        fetch(
          leoUrlRoot + '/api/google/v1/apps?includeDeleted=false',
          fetchOpts()
        ).then((r) => r.json()),
    },
    Metrics: { captureEvent: () => undefined },
    Disks: {
      list: () =>
        fetch(
          leoUrlRoot + '/api/google/v1/disks?includeDeleted=false',
          fetchOpts()
        ).then((r) => r.json()),
    },
  };
};

interface RuntimesListProps
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps {
  getAccessToken: () => String;
}
export const RuntimesList = fp.flow(
  withNavigation,
  withRouter
)(
  class extends React.Component<RuntimesListProps> {
    static displayName = 'RuntimesList';

    async componentDidMount() {
      this.props.hideSpinner();
    }

    render() {
      return (
        <>
          {/* @ts-ignore // only a few of the properties of the Ajax object are bound */}
          <ajaxContext.Provider value={ajax(this.props.getAccessToken)}>
            <Environments
              {...{
                nav: {
                  getLink: (_, { namespace }) => `/workspaces/${namespace}`,
                },
              }}
            />
          </ajaxContext.Provider>
        </>
      );
    }
  }
);
