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

const hiddenTableColumns = [
  {
    tableName: 'cloud environments',
    columnIndexesToHide: [workspace, deleteCloudEnvironment],
  },
  {
    tableName: 'persistent disks',
    columnIndexesToHide: [workspace, pdStatus],
  },
];

const ajax = (signal) => {
  const jsonLeoFetch = (path, method = 'get') =>
    fetch(environment.leoApiUrl + path, {
      signal,
      method,
      headers: { authorization: 'bearer ' + getAccessToken() },
    }).then((r) => r.json());
  return {
    Workspaces: {
      list: () => workspacesApi().getWorkspaces(),
    },
    Runtimes: {
      listV2: () => jsonLeoFetch('/api/v2/runtimes?role=creator'),
    },
    Apps: {
      listWithoutProject: () =>
        jsonLeoFetch('/api/google/v1/apps?role=creator&includeDeleted=false'),
    },
    Metrics: { captureEvent: () => undefined },
    Disks: {
      disksV1: () => ({
        list: () => jsonLeoFetch('/api/google/v1/disks?role=creator'),
        disk: (googleProject, name) => ({
          delete: () =>
            jsonLeoFetch(
              `/api/google/v1/disks/${googleProject}/${name}`,
              'delete'
            ).catch(() => {
              window.location.reload();
            }),
        }),
      }),
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
    }
    
    /* hides the "Hide resources you did not create" checkbox, which does not do anything */
    [role=checkbox] {
        display: none !important
    }

    /* hides the text after the checkbox above */
    [role=checkbox] ~ span {
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
