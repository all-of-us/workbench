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

// Indexes of hidden columns
const workspace = 2;
const deleteCloudEnvironment = 11;
const pdStatus = 5;
const pdDelete = 10;

const hiddenTableColumns = [
  {
    tableName: 'cloud environments',
    columnIndexesToHide: [workspace, deleteCloudEnvironment],
  },
  {
    tableName: 'persistent disks',
    columnIndexesToHide: [workspace, pdStatus, pdDelete],
  },
];

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
  hiddenTableColumns.map(({ tableName, columnIndexesToHide }) =>
    columnIndexesToHide
      .map(
        (columnIndex) =>
          `div[aria-label="` +
          tableName +
          `"] div[role="columnheader"]:nth-child(` +
          columnIndex +
          `),
       div[aria-label="` +
          tableName +
          `"] .table-cell:nth-child(` +
          columnIndex +
          `)`
      )
      .join(',\n')
  ) +
  ` {
         display: none !important
        }
      
       div[style*="z-index: 2"]:has(>div>svg) {
          display: none !important
        }`;

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
