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
  ResourceType,
  Workspace,
  WorkspaceResource,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import colors from 'app/styles/colors';
import { reactStyles, withCdrVersions } from 'app/utils';
import { displayDate, displayDateWithoutHours } from 'app/utils/dates';
import {
  getDisplayName,
  getType,
  getTypeString,
  isNotebook,
} from 'app/utils/resources';

import { getCdrVersion } from './cdr-versions';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from './constants';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '130px',
  },
  modifiedDateColumn: {
    textAlign: 'left',
    width: '9rem',
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
  resourceType: string;
  resourceName: string;
  formattedLastModified: string;
  lastModifiedDateAsString: string;
  lastModifiedBy: string;
  workspaceName: string;
  cdrVersionName: string;
  resource: WorkspaceResource;
}

interface Props {
  existingNameList: string[];
  workspaceResources: WorkspaceResource[];
  onUpdate: Function;
  workspaceMap?: Map<string, Workspace>;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  recentResourceSource?: boolean;
}

export const ResourcesList = fp.flow(withCdrVersions())((props: Props) => {
  const [tableData, setTableData] = useState<TableData[]>();

  const reloadResources = async () => {
    await props.onUpdate();
  };

  const getResourceMap = () => {
    const resourceTypeNameListMap = new Map<ResourceType, string[]>();
    props.workspaceResources.map((resource) => {
      const resourceType = getType(resource);
      const resourceName = getDisplayName(resource);
      const resourceNameList = resourceTypeNameListMap.get(resourceType);
      const keyValue = !!resourceNameList
        ? [...resourceNameList, resourceName]
        : [resourceName];
      resourceTypeNameListMap.set(resourceType, keyValue);
    });
    return resourceTypeNameListMap;
  };
  const resourceTypeNameMap = getResourceMap();

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: resourceTypeNameMap.get(getType(resource)),
      onUpdate: reloadResources,
    });
  };

  const getWorkspace = (r: WorkspaceResource) => {
    const { workspaceMap } = props;
    return workspaceMap?.get(r.workspaceFirecloudName);
  };

  const getCdrVersionName = (r: WorkspaceResource) => {
    const { cdrVersionTiersResponse, recentResourceSource } = props;
    if (!recentResourceSource) {
      return '';
    }
    const cdrVersion = getCdrVersion(getWorkspace(r), cdrVersionTiersResponse);
    return cdrVersion?.name;
  };

  const addAdminLockToNameColumn = () => {
    return (
      <TooltipTrigger content={<div>Workspace compliance action required</div>}>
        <FontAwesomeIcon
          style={{ color: colors.warning, marginRight: '0.5rem' }}
          size={'sm'}
          icon={faLockAlt}
        />
      </TooltipTrigger>
    );
  };

  useEffect(() => {
    const { workspaceResources } = props;
    if (workspaceResources) {
      setTableData(
        workspaceResources.map((r) => {
          return {
            resource: r,
            menu: renderResourceMenu(r),
            resourceType: getTypeString(r),
            resourceName: getDisplayName(r),
            formattedLastModified: displayDateWithoutHours(
              r.lastModifiedEpochMillis
            ),
            lastModifiedDateAsString: displayDate(r.lastModifiedEpochMillis),
            cdrVersionName: getCdrVersionName(r),
            workspaceName: getWorkspace(r).name,
            lastModifiedBy: r.lastModifiedBy,
          };
        })
      );
    }
  }, [props.workspaceResources]);

  const displayWorkspace = (rowData) => {
    return (
      <WorkspaceNavigation
        workspace={getWorkspace(rowData.resource)}
        resource={rowData.resource}
        style={styles.navigation}
      />
    );
  };

  const displayResourceType = (rowData) => {
    const { resource } = rowData;
    return (
      <ResourceNavigation resource={resource}>
        <StyledResourceType resource={resource} />
      </ResourceNavigation>
    );
  };

  const displayResourceName = (rowData) => {
    const { resource } = rowData;
    return (
      <ResourceNavigation resource={resource} style={styles.navigation}>
        {resource.adminLocked && addAdminLockToNameColumn()}
        {getDisplayName(resource)}
      </ResourceNavigation>
    );
  };

  return (
    <React.Fragment>
      <div data-test-id='resources-table'>
        {tableData?.length > 0 && (
          <DataTable
            data-test-id='resource-list'
            value={tableData}
            scrollable={true}
            sortMode='multiple'
            paginator
            rows={ROWS_PER_PAGE_RESOURCE_TABLE}
          >
            <Column field='menu' style={styles.menu} />
            <Column
              field='resourceType'
              body={displayResourceType}
              header='Item type'
              sortable
              style={styles.typeColumn}
            />
            <Column
              field='resourceName'
              header='Name'
              body={displayResourceName}
              style={styles.column}
              sortable
              filter
              filterPlaceholder={'Search Name'}
            />
            {props.recentResourceSource && (
              <Column
                field='workspaceName'
                header='Workspace name'
                sortable
                body={displayWorkspace}
                style={styles.column}
              />
            )}
            <Column
              field='formattedLastModified'
              header='Last Modified Date'
              style={styles.modifiedDateColumn}
              sortField={'lastModifiedDateAsString'}
              sortable
            />
            {props.recentResourceSource && (
              <Column
                field='cdrVersionName'
                header='Dataset'
                style={styles.column}
              />
            )}
            <Column
              field='lastModifiedBy'
              header='Last Modified By'
              style={styles.column}
            />
          </DataTable>
        )}
      </div>
    </React.Fragment>
  );
});
