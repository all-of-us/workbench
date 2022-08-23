import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { WorkspaceResource } from 'generated/fetch';

import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import { reactStyles, withCdrVersions } from 'app/utils';
import { displayDateWithoutHours } from 'app/utils/dates';
import { getDisplayName } from 'app/utils/resources';

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

interface TableData {
  menu: JSX.Element;
  resourceType: JSX.Element;
  resourceName: JSX.Element;
  formattedLastModified: string;
}

interface Props {
  workspaceResources: WorkspaceResource[];
  onUpdate: Function;
}

export const ResourcesList = fp.flow(withCdrVersions())((props: Props) => {
  const [tableData, setTableData] = useState<TableData[]>();

  const reloadResources = async () => {
    await props.onUpdate();
  };

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: [], // TODO existing bug RW-5847: does not populate names for rename modal
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
            formattedLastModified: displayDateWithoutHours(
              r.lastModifiedEpochMillis
            ),
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
      <div data-test-id='recent-resources-table'>
        {tableData?.length > 0 && (
          <DataTable
            data-test-id='resource-list'
            value={tableData}
            scrollable={true}
            sortMode='multiple'
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
              sortable
              filter
              filterPlaceholder={'Search Name'}
              filterMatchMode='custom'
              filterFunction={filterNameColumn}
            />
            <Column
              field='formattedLastModified'
              header='Last changed'
              style={styles.column}
            />
          </DataTable>
        )}
      </div>
    </React.Fragment>
  );
});
