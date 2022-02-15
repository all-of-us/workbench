import * as React from 'react';
import { CSSProperties, useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { faLockAlt } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  CdrVersionTiersResponse,
  Workspace,
  WorkspaceResource,
  WorkspaceResourceResponse,
  WorkspaceResponse,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { SmallHeader } from 'app/components/headers';
import { ExclamationTriangle } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import { SpinnerOverlay } from 'app/components/spinners';
import { userMetricsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, withCdrVersions } from 'app/utils';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { displayDateWithoutHours } from 'app/utils/dates';
import { convertAPIError } from 'app/utils/errors';
import { getDisplayName, isNotebook } from 'app/utils/resources';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '130px',
  },
  menu: {
    width: '30px',
  },
  navigation: {
    fontFamily: 'Montserrat',
    fontSize: '14px',
    letterSpacing: 0,
    lineHeight: '22px',
  },
  error: {
    width: '99%',
    marginTop: '2.75rem',
    padding: '0.25rem',
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    borderRadius: '5px',
  },
});

interface NavProps {
  workspace: Workspace;
  resource: WorkspaceResource;
  style?: CSSProperties;
}

const WorkspaceNavigation = (props: NavProps) => {
  const {
    workspace: { name, namespace, id },
    resource,
    style,
  } = props;
  const tab = isNotebook(resource) ? 'notebooks' : 'data';
  const url = `/workspaces/${namespace}/${id}/${tab}`;

  return (
    <Clickable>
      <RouterLink to={url} style={style} data-test-id='workspace-navigation'>
        {name}
      </RouterLink>
    </Clickable>
  );
};

interface TableData {
  menu: JSX.Element;
  resourceType: JSX.Element;
  resourceName: JSX.Element;
  workspaceName: JSX.Element;
  formattedLastModified: string;
  cdrVersionName: string;
}

interface Props {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  workspaces: WorkspaceResponse[];
}

export const RecentResources = fp.flow(withCdrVersions())((props: Props) => {
  const [loading, setLoading] = useState(true);
  const [resources, setResources] = useState<WorkspaceResourceResponse>();
  const [wsMap, setWorkspaceMap] = useState<Map<string, Workspace>>();
  const [tableData, setTableData] = useState<TableData[]>();
  const [apiError, setApiError] = useState(false);

  const loadResources = async () => {
    setLoading(true);
    await userMetricsApi()
      .getUserRecentResources()
      .then(setResources)
      .catch(async (e) => {
        const response = await convertAPIError(e);
        console.error(response.message);
        setApiError(true);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const { workspaces } = props;
    if (workspaces) {
      const workspaceTuples = workspaces.map(
        (r) => [r.workspace.id, r.workspace] as [string, Workspace]
      );
      setWorkspaceMap(new Map(workspaceTuples));
      loadResources();
    }
  }, [props.workspaces]);

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: [], // TODO existing bug RW-5847: does not populate names for rename modal
      onUpdate: loadResources,
    });
  };

  useEffect(() => {
    const getWorkspace = (r: WorkspaceResource) => {
      return wsMap.get(r.workspaceFirecloudName);
    };

    const addAdminLockToNameColumn = () => {
      return (
        <TooltipTrigger
          content={<div>Workspace compliance action required</div>}
        >
          <FontAwesomeIcon
            style={{ color: colors.warning, marginRight: '0.5rem' }}
            size={'sm'}
            icon={faLockAlt}
          />
        </TooltipTrigger>
      );
    };

    const getCdrVersionName = (r: WorkspaceResource) => {
      const { cdrVersionTiersResponse } = props;
      const cdrVersion = getCdrVersion(
        getWorkspace(r),
        cdrVersionTiersResponse
      );
      return cdrVersion?.name;
    };

    if (resources && wsMap) {
      setTableData(
        resources
          .filter((r) => wsMap.has(r.workspaceFirecloudName))
          .map((r) => {
            return {
              menu: renderResourceMenu(r),
              resourceType: (
                <ResourceNavigation resource={r}>
                  <StyledResourceType resource={r} />
                </ResourceNavigation>
              ),
              resourceName: (
                <ResourceNavigation resource={r} style={styles.navigation}>
                  {r.adminLocked && addAdminLockToNameColumn()}
                  {getDisplayName(r)}
                </ResourceNavigation>
              ),
              workspaceName: (
                <WorkspaceNavigation
                  workspace={getWorkspace(r)}
                  resource={r}
                  style={styles.navigation}
                />
              ),
              formattedLastModified: displayDateWithoutHours(
                r.lastModifiedEpochMillis
              ),
              cdrVersionName: getCdrVersionName(r),
            };
          })
      );
    }
  }, [resources, wsMap]);

  return resources && wsMap && !loading ? (
    <React.Fragment>
      <SmallHeader>Recently Accessed Items</SmallHeader>
      {apiError ? (
        <div>
          <ExclamationTriangle
            color={colors.warning}
            style={{ height: '1.5rem', width: '1.5rem' }}
          />
          <div style={{ ...styles.error }}>
            Sorry, loading of recent resources request cannot be completed.
          </div>
        </div>
      ) : (
        <div data-test-id='recent-resources-table'>
          <DataTable
            value={tableData}
            scrollable={true}
            paginator={true}
            paginatorTemplate='CurrentPageReport'
            currentPageReportTemplate='Showing {totalRecords} most recent items'
          >
            <Column field='menu' style={styles.menu} />
            <Column
              field='resourceType'
              header='Item type'
              style={styles.typeColumn}
            />
            <Column field='resourceName' header='Name' style={styles.column} />
            <Column
              field='workspaceName'
              header='Workspace name'
              style={styles.column}
            />
            <Column
              field='formattedLastModified'
              header='Last changed'
              style={styles.column}
            />
            <Column
              field='cdrVersionName'
              header='Dataset'
              style={styles.column}
            />
          </DataTable>
        </div>
      )}
    </React.Fragment>
  ) : (
    loading && <SpinnerOverlay />
  );
});
