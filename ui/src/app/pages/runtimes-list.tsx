import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { environment } from 'environments/environment';
import { WarningMessage } from 'app/components/messages';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { getAccessToken } from 'app/utils/authentication';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { ajaxContext, Environments } from 'terraui/out/Environments';

// Indexes of hidden columns
const deleteCloudEnvironment = 11;

const hiddenTableColumns = [
  {
    tableName: 'cloud environments',
    columnIndexesToHide: [deleteCloudEnvironment],
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
      list: () =>
        workspacesApi()
          .getWorkspaces()
          .then((res) => res.items),
    },
    Runtimes: {
      listV2: () => jsonLeoFetch('/api/v2/runtimes?role=creator'),
    },
    Apps: {
      listWithoutProject: () =>
        jsonLeoFetch(
          '/api/google/v1/apps?role=creator&includeDeleted=false&includeLabels=saturnWorkspaceNamespace,saturnWorkspaceName'
        ),
    },
    Metrics: { captureEvent: () => undefined },
    Disks: {
      disksV1: () => ({
        list: () =>
          jsonLeoFetch(
            '/api/google/v1/disks?role=creator&includeLabels=saturnWorkspaceNamespace,saturnWorkspaceName'
          ),
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
    
    /* hides a warning banner which is only appropriate for Terra UI */
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

const stringToSlug = (s) => s.toLowerCase().replace(/\s+/g, '');

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
          <WarningMessage>
            <b>About the Cloud Environments Page</b>
            <br />
            Below you can see resources (cloud environments and disks) that
            you’ve created across all workspaces in the All of Us Researcher
            Workbench. Please note that this feature is still in development.
            Please use the "Contact Us" link to provide feedback.
          </WarningMessage>
          {/* @ts-ignore // only a few of the properties of the Ajax object are bound */}
          <ajaxContext.Provider value={ajax}>
            <Environments
              {...{
                nav: {
                  // called from, for example:
                  // https://github.com/DataBiosphere/terra-ui/blob/4333c7b94d6ce10a6fe079361e98c2b6cc71f83a/src/pages/Environments.js#L420
                  getLink: (_, { namespace, name }) =>
                    `/workspaces/${namespace}/${stringToSlug(name)}/analysis`,
                },
              }}
            />
          </ajaxContext.Provider>
        </>
      );
    }
  }
);
