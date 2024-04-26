import * as React from 'react';
import { CSSProperties, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { faLock } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  CdrVersionTiersResponse,
  ResourceType,
  Workspace,
  WorkspaceResource,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { ResourceListActionMenu } from 'app/components/resources/resource-list-action-menu';
import { analysisTabPath, dataTabPath } from 'app/routing/utils';
import colors from 'app/styles/colors';
import { reactStyles, withCdrVersions } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from 'app/utils/constants';
import { displayDateWithoutHours } from 'app/utils/dates';
import {
  getDisplayName,
  getType,
  getTypeString,
  isNotebook,
} from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

import { ResourceNavigation } from './resource-navigation';
import { StyledResourceType } from './styled-resource-type';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '140px',
  },
  modifiedDateColumn: {
    textAlign: 'left',
    width: '13.5rem',
  },
  menu: {
    width: '30px',
  },
  navigation: {
    fontFamily: 'Montserrat',
    fontSize: '14px',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    display: 'block',
    overflow: 'hidden',
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
  const url = isNotebook(resource)
    ? analysisTabPath(namespace, id)
    : dataTabPath(namespace, id);
  return (
    <Clickable>
      <Link to={url} style={style} data-test-id='workspace-navigation'>
        {name}
      </Link>
    </Clickable>
  );
};

interface TableData {
  menu: JSX.Element;
  resourceType: string;
  resourceName: string;
  lastModifiedForSorting: number;
  formattedLastModified: string;
  lastModifiedBy: string;
  cdrVersionName: string;
  resource: WorkspaceResource;
  workspace: Workspace;
}

interface Props {
  existingNameList: string[];
  workspaceResources: WorkspaceResource[];
  onUpdate: () => Promise<void>;
  workspaces: WorkspaceData[];
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  recentResourceSource?: boolean;
}

export const ResourceList = fp.flow(withCdrVersions())((props: Props) => {
  const [tableData, setTableData] = useState<TableData[]>();

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

  const getCdrVersionName = (r: WorkspaceResource) => {
    const { cdrVersionTiersResponse } = props;

    const cdrVersion = findCdrVersion(r.cdrVersionId, cdrVersionTiersResponse);
    return cdrVersion?.name;
  };

  const addAdminLockToNameColumn = () => {
    return (
      <TooltipTrigger content={<div>Workspace compliance action required</div>}>
        <FontAwesomeIcon
          style={{ color: colors.warning, marginRight: '0.75rem' }}
          size={'sm'}
          icon={faLock}
        />
      </TooltipTrigger>
    );
  };

  useEffect(() => {
    const { workspaces, workspaceResources } = props;
    if (workspaceResources) {
      setTableData(
        fp.flatMap((r) => {
          const workspace = workspaces.find(
            (w) => w.namespace === r.workspaceNamespace
          );

          // Don't return resources where we no longer have access to the workspace.
          // For example: the owner has unshared the workspace, but a recent-resource entry remains.
          return workspace
            ? [
                {
                  resource: r,
                  workspace,
                  menu: (
                    <ResourceListActionMenu
                      {...{ workspace }}
                      resource={r}
                      existingNameList={resourceTypeNameMap.get(getType(r))}
                      onUpdate={props.onUpdate}
                    />
                  ),
                  resourceType: getTypeString(r),
                  resourceName: getDisplayName(r),
                  lastModifiedForSorting: r.lastModifiedEpochMillis,
                  formattedLastModified: displayDateWithoutHours(
                    r.lastModifiedEpochMillis
                  ),
                  cdrVersionName: getCdrVersionName(r),
                  lastModifiedBy: r.lastModifiedBy,
                },
              ]
            : [];
        }, workspaceResources)
      );
    }
  }, [props.workspaceResources]);

  const displayWorkspace = (rowData) => {
    const { workspace, resource } = rowData;
    return (
      <WorkspaceNavigation
        {...{ workspace, resource }}
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
    const displayName = getDisplayName(resource);
    return (
      <ResourceNavigation resource={resource} style={styles.navigation}>
        {resource.adminLocked && addAdminLockToNameColumn()}
        <TooltipTrigger content={displayName}>
          <span>{displayName}</span>
        </TooltipTrigger>
      </ResourceNavigation>
    );
  };

  return (
    <React.Fragment>
      <div data-test-id='resources-table' style={{ flex: 1 }}>
        {tableData?.length > 0 && (
          <DataTable
            filterDisplay='row'
            data-test-id='resource-list'
            value={tableData}
            sortMode='multiple'
            paginator
            breakpoint='0px'
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
              sortField='lastModifiedForSorting'
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
