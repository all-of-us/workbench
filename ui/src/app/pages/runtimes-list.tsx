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

// Runtime-List tables hidden columns
const WORKSPACE_NAME_COLUMN_INDEX = 2;
const DELETE_CLOUD_ENV_COLUMN_INDEX = 11;
const PD_STATUS_COLUMN_INDEX = 5;
const DELETE_PD_COLUMN_INDEX = 10;

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

const css =
  `
div[aria-label="cloud environments"] 
div[role="columnheader"]:nth-child(` +
  WORKSPACE_NAME_COLUMN_INDEX +
  `), 
div[aria-label="cloud environments"] 
div[role="columnheader"]:nth-child(` +
  DELETE_CLOUD_ENV_COLUMN_INDEX +
  `),
div[aria-label="cloud environments"] 
.table-cell:nth-child(` +
  WORKSPACE_NAME_COLUMN_INDEX +
  `),
div[aria-label="cloud environments"] 
.table-cell:nth-child(` +
  DELETE_CLOUD_ENV_COLUMN_INDEX +
  `){
   display: none !important
}

div[aria-label="persistent disks"] 
div[role="columnheader"]:nth-child(` +
  WORKSPACE_NAME_COLUMN_INDEX +
  `),
div[aria-label="persistent disks"] 
div[role="columnheader"]:nth-child(` +
  PD_STATUS_COLUMN_INDEX +
  `),
div[aria-label="persistent disks"] 
div[role="columnheader"]:nth-child(` +
  DELETE_PD_COLUMN_INDEX +
  `), 
div[aria-label="persistent disks"] 
.table-cell:nth-child(` +
  WORKSPACE_NAME_COLUMN_INDEX +
  `),
div[aria-label="persistent disks"] 
.table-cell:nth-child(` +
  PD_STATUS_COLUMN_INDEX +
  `),
div[aria-label="persistent disks"] 
.table-cell:nth-child(` +
  DELETE_PD_COLUMN_INDEX +
  `)
{
   display: none !important
}

div[style*="z-index: 2"]:has(>div>svg) {
  display: none !important
}

`;

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
          <style>{css}</style>
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
