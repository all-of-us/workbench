import * as React from 'react';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { FileDetail } from 'generated/fetch';

import { AppLogo } from 'app/components/apps-panel/app-logo';
import { Clickable } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { withErrorModal } from 'app/components/modals';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { NotebookResourceCard } from 'app/pages/analysis/notebook-resource-card';
import { appsExtensionMap, listNotebooks } from 'app/pages/analysis/util';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { displayDateWithoutHours } from 'app/utils/dates';
import { convertToResources } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

import { AppSelector } from './app-selector';

const styles = reactStyles({
  fadeBox: {
    margin: 'auto',
    marginTop: '1.5rem',
    width: '95.7%',
    height: 8,
    border: `solid ${colorWithWhiteness(colors.disabled, 0.6)}`,
    borderWidth: '1px 1px 0 1px',
    borderRadius: '8px 8px 0 0',
  },
  spacing: {
    marginBottom: '32px',
    backgroundColor: 'white',
    paddingTop: '0.5rem',
    paddingBottom: '0.5rem',
  },
  tableHeader: {
    borderLeft: 'none',
    borderRight: 'none',
  },
  columns: {
    borderLeft: 'none',
    borderRight: 'none',
    borderBottomWidth: 16,
    borderTopWidth: 0,
    borderBlockColor: colorWithWhiteness(colors.disabled, 0.9),
    paddingBottom: 0,
    lineHeight: 1,
  },
  rows: {
    backgroundColor: 'white',
    paddingTop: 0,
  },
});

interface AppFilesListProps extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}
export const AppFilesList = withCurrentWorkspace()(
  (props: AppFilesListProps) => {
    const { workspace } = props;

    const [filesList, setFilesList] = useState<FileDetail[]>();

    const loadNotebooks = withErrorModal(
      {
        title: 'Error Loading Files',
        message: 'Please refresh to try again.',
        onDismiss: () => props.hideSpinner(),
      },
      async () => {
        props.showSpinner();
        listNotebooks(workspace).then(setFilesList);
        props.hideSpinner();
      }
    );

    useEffect(() => {
      if (workspace) {
        loadNotebooks();
      }
    }, []);

    const displayMenu = (row) => {
      return (
        <NotebookResourceCard
          resource={convertToResources([row], props.workspace)[0]}
          workspace
          menuOnly
          appsAnalysis
          existingNameList={filesList.map((file) => file.name)}
          onUpdate={loadNotebooks}
        />
      );
    };

    const displayAppLogo = (row) => {
      // Find App Type on the basis of file name extension
      const fileName = row.name;
      const application = appsExtensionMap.find((app) =>
        fileName.endsWith(app.extension)
      );
      return (
        application && (
          <AppLogo
            appType={application.appType}
            style={{ marginRight: '1em' }}
          />
        )
      );
    };

    const displayName = (row) => {
      const {
        workspace: { namespace, id },
      } = props;
      const url = `/workspaces/${namespace}/${id}/notebooks/preview/${row.name}`;
      return (
        <Clickable>
          <RouterLink to={url} data-test-id='notebook-navigation'>
            {row.name}
          </RouterLink>
        </Clickable>
      );
    };

    const displayLastModifiedTime = (row) => {
      const time = displayDateWithoutHours(row.lastModifiedTime);
      return <div>{time}</div>;
    };

    return (
      <FadeBox style={styles.fadeBox}>
        <FlexColumn>
          <FlexRow style={styles.spacing}>
            <ListPageHeader
              style={{
                paddingTop: '1rem',
                paddingRight: '2.25rem',
                paddingLeft: '2rem',
              }}
            >
              Your Analyses
            </ListPageHeader>
            <AppSelector {...{ workspace }} />
          </FlexRow>
          {workspace && filesList && (
            <DataTable filterDisplay='menu' value={filesList}>
              <Column
                style={styles.columns}
                body={displayMenu}
                bodyStyle={styles.rows}
              />
              <Column
                header='Application'
                body={displayAppLogo}
                style={styles.columns}
                bodyStyle={styles.rows}
              />
              <Column
                style={styles.columns}
                header='Name'
                body={displayName}
                bodyStyle={styles.rows}
                filter
                filterPlaceholder={'Search Name'}
                sortable
              />
              <Column
                style={styles.columns}
                bodyStyle={styles.rows}
                header='Last Modified Time'
                body={displayLastModifiedTime}
              />
              <Column
                style={styles.columns}
                bodyStyle={styles.rows}
                field='lastModifiedBy'
                sortable
                header='Last Modified By'
              />
            </DataTable>
          )}
        </FlexColumn>
      </FadeBox>
    );
  }
);
