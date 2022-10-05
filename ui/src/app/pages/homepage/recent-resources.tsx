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

import { AlertWarning } from 'app/components/alert';
import { Clickable } from 'app/components/buttons';
import { SmallHeader } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import { SpinnerOverlay } from 'app/components/spinners';
import { userMetricsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { cond, reactStyles, withCdrVersions } from 'app/utils';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { displayDate, displayDateWithoutHours } from 'app/utils/dates';
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
  resourceNameAsString: string;
  workspaceName: JSX.Element;
  workspaceNameAsString: string;
  formattedLastModified: string;
  lastModifiedDateAsString: string;
  cdrVersionName: string;
  lastModifiedBy: string;
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
  const [apiLoadError, setApiLoadError] = useState<string>(null);

  const loadResources = async () => {
    setLoading(true);
    await userMetricsApi()
      .getUserRecentResources()
      .then(setResources)
      .catch(() => {
        setApiLoadError(
          'An error occurred while loading recent resources. Please refresh the page to reload.'
        );
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
              resourceNameAsString: getDisplayName(r),
              workspaceName: (
                <WorkspaceNavigation
                  workspace={getWorkspace(r)}
                  resource={r}
                  style={styles.navigation}
                />
              ),
              workspaceNameAsString: getWorkspace(r).name,
              formattedLastModified: displayDateWithoutHours(
                r.lastModifiedEpochMillis
              ),
              cdrVersionName: getCdrVersionName(r),
              lastModifiedBy: r.lastModifiedBy,
              lastModifiedDateAsString: displayDate(r.lastModifiedEpochMillis),
            };
          })
      );
    }
  }, [resources, wsMap]);

  return cond(
    [
      apiLoadError && !loading,
      () => (
        <AlertWarning style={{ fontSize: 14 }}>
          <ClrIcon className='is-solid' shape='exclamation-triangle' />
          {apiLoadError}
        </AlertWarning>
      ),
    ],
    [
      resources && resources.length > 0 && wsMap && !loading,
      () => (
        <React.Fragment>
          <SmallHeader>Recently Accessed Items</SmallHeader>
          <div data-test-id='recent-resources-table'>
            <DataTable
              value={tableData}
              scrollable={true}
              paginator={true}
              sortMode='multiple'
              paginatorTemplate='CurrentPageReport'
              currentPageReportTemplate='Showing {totalRecords} most recent items'
              style={{ width: '65rem', border: 'none' }}
            >
              <Column field='menu' style={styles.menu} />
              <Column
                field='resourceType'
                header='Item type'
                style={styles.typeColumn}
              />
              <Column
                field='resourceName'
                header='Name'
                style={styles.column}
                sortField='resourceNameAsString'
                sortable
              />
              <Column
                field='workspaceName'
                header='Workspace name'
                sortField='workspaceNameAsString'
                sortable
                style={styles.column}
              />
              <Column
                field='cdrVersionName'
                header='Dataset Version'
                style={{ ...styles.column, width: '8rem' }}
              />
              <Column
                field='formattedLastModified'
                header='Last changed'
                sortField='lastModifiedDateAsString'
                sortable
                style={{ ...styles.column, width: '6.5rem' }}
              />
              <Column
                field='lastModifiedBy'
                header='Last Modified By'
                style={styles.column}
              />
            </DataTable>
          </div>
        </React.Fragment>
      ),
    ],
    [resources && resources.length === 0, () => <div></div>],
    [loading, () => <SpinnerOverlay />]
  );
});
