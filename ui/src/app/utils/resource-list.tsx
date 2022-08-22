import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { faLockAlt } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { CdrVersionTiersResponse, WorkspaceResource } from 'generated/fetch';

import { TooltipTrigger } from 'app/components/popups';
import { renderResourceCard } from 'app/components/render-resource-card';
import {
  ResourceNavigation,
  StyledResourceType,
} from 'app/components/resource-card';
import colors from 'app/styles/colors';
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
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  workspaces: WorkspaceResource[];
  onUpdate: Function;
}

export const ResourcesList = fp.flow(withCdrVersions())((props: Props) => {
  const [tableData, setTableData] = useState<TableData[]>();

  const loadResources = async () => {
    await props.onUpdate();
  };

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: [], // TODO existing bug RW-5847: does not populate names for rename modal
      onUpdate: loadResources,
    });
  };

  useEffect(() => {
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

    if (props.workspaces) {
      setTableData(
        props.workspaces.map((r) => {
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
            formattedLastModified: displayDateWithoutHours(
              r.lastModifiedEpochMillis
            ),
          };
        })
      );
    }
  }, [props.workspaces]);

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
