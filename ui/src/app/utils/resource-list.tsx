import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { ResourceType, WorkspaceResource } from 'generated/fetch';

import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import { reactStyles, withCdrVersions } from 'app/utils';
import { displayDate, displayDateWithoutHours } from 'app/utils/dates';
import { getDisplayName, getType } from 'app/utils/resources';

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

interface TableData {
  menu: JSX.Element;
  resourceType: JSX.Element;
  resourceName: JSX.Element;
  resourceNameAsString: string;
  formattedLastModified: string;
  lastModifiedDateAsString: string;
}

interface Props {
  existingNameList: string[];
  workspaceResources: WorkspaceResource[];
  onUpdate: Function;
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

  useEffect(() => {
    const { workspaceResources } = props;
    if (workspaceResources) {
      setTableData(
        workspaceResources.map((r) => {
          return {
            menu: renderResourceMenu(r),
            resourceType: (
              <ResourceNavigation resource={r}>
                <StyledResourceType resource={r} />
              </ResourceNavigation>
            ),
            resourceName: (
              <ResourceNavigation resource={r} style={styles.navigation}>
                {getDisplayName(r)}
              </ResourceNavigation>
            ),
            resourceNameAsString: getDisplayName(r),
            formattedLastModified: displayDateWithoutHours(
              r.lastModifiedEpochMillis
            ),
            lastModifiedDateAsString: displayDate(r.lastModifiedEpochMillis),
          };
        })
      );
    }
  }, [props.workspaceResources]);

  const filterNameColumn = (value, filter) => {
    return value.props.children.toUpperCase().startsWith(filter.toUpperCase());
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
            style={{ width: '44rem' }}
            paginator
            rows={ROWS_PER_PAGE_RESOURCE_TABLE}
          >
            <Column field='menu' style={styles.menu} />
            <Column
              field='resourceType'
              header='Item type'
              sortable
              style={styles.typeColumn}
            />
            <Column
              field='resourceName'
              header='Name'
              style={styles.column}
              sortField={'resourceNameAsString'}
              sortable
              filter
              filterPlaceholder={'Search Name'}
              filterMatchMode='custom'
              filterFunction={filterNameColumn}
            />
            <Column
              field='formattedLastModified'
              header='Last Modified Date'
              style={styles.modifiedDateColumn}
              sortField={'lastModifiedDateAsString'}
              sortable
            />
          </DataTable>
        )}
      </div>
    </React.Fragment>
  );
});
