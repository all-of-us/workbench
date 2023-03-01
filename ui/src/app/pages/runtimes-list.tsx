import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { environment } from 'environments/environment';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { getAccessToken } from 'app/utils/authentication';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { ajaxContext, Environments } from 'terraui/out/Environments';

const ajax = (signal) => {
  const jsonLeoFetch = (path) =>
    fetch(environment.leoApiUrl + path, {
      signal,
      headers: { authorization: 'bearer ' + getAccessToken() },
    }).then((r) => r.json());
  return {
    Workspaces: {
      list: () => workspacesApi().getWorkspaces(),
    },
    Runtimes: {
      listV2: () => jsonLeoFetch('/api/v2/runtimes?includeDeleted=false'),
    },
    Apps: {
      listWithoutProject: () =>
        jsonLeoFetch('/api/google/v1/apps?includeDeleted=false'),
    },
    Metrics: { captureEvent: () => undefined },
    Disks: {
      list: () => jsonLeoFetch('/api/google/v1/disks?includeDeleted=false'),
    },
  };
};

interface RuntimesListProps
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps {}
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
          <ajaxContext.Provider value={ajax}>
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
