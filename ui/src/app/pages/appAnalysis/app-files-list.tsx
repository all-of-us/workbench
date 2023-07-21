import * as React from 'react';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { FileDetail } from 'generated/fetch';

import { AppLogo } from 'app/components/apps-panel/app-logo';
import { Clickable, KebabCircleButton } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { withErrorModal } from 'app/components/modals';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { NotebookResourceCard } from 'app/pages/analysis/notebook-resource-card';
import { getAppInfoFromFileName, listNotebooks } from 'app/pages/analysis/util';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { NOTEBOOKS_TAB_NAME } from 'app/utils/constants';
import { displayDateWithoutHours } from 'app/utils/dates';
import { convertToResources } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

import { AppSelector } from './app-selector';

const styles = reactStyles({
  fadeBox: {
    margin: 'auto',
    marginTop: '1.5rem',
    width: '95.7%',
    border: `solid ${colorWithWhiteness(colors.disabled, 0.6)}`,
    borderWidth: '1px 1px 0 1px',
    borderRadius: '8px 8px 0 0',
    minHeight: '650px', // kluge to give more space to the config panel
  },
  spacing: {
    marginBottom: '32px',
    backgroundColor: 'white',
    paddingTop: '0.5rem',
    paddingBottom: '0.5rem',
  },
  tableHeader: {
    paddingTop: '1.5rem',
    paddingBottom: '0.5rem',
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
    }, [workspace]);

    const displayMenu = (row) => {
      return (
        <NotebookResourceCard
          resource={convertToResources([row], props.workspace)[0]}
          workspace
          menuOnly
          menuButtonComponentOverride={KebabCircleButton}
          existingNameList={filesList.map((file) => file.name)}
          onUpdate={loadNotebooks}
        />
      );
    };

    const displayAppLogo = (row) => {
      // Find App Type on the basis of file name extension
      const { name } = row;
      const appType = getAppInfoFromFileName(name).appType;
      return <AppLogo appType={appType} style={{ marginRight: '1em' }} />;
    };

    const displayName = (row) => {
      const {
        workspace: { namespace, id },
      } = props;
      const { name } = row;
      const url = `/workspaces/${namespace}/${id}/${NOTEBOOKS_TAB_NAME}/preview/${name}`;
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
            <DataTable
              data-test-id='apps-file-list'
              filterDisplay='row'
              value={filesList}
              paginator
              rows={5}
              style={{ paddingBottom: '10rem' }}
            >
              <Column
                headerStyle={styles.tableHeader}
                style={styles.columns}
                body={displayMenu}
                bodyStyle={styles.rows}
              />
              <Column
                headerStyle={styles.tableHeader}
                header='Application'
                body={displayAppLogo}
                style={styles.columns}
                bodyStyle={styles.rows}
              />
              <Column
                style={styles.columns}
                headerStyle={styles.tableHeader}
                header='Name'
                field={'name'}
                body={displayName}
                bodyStyle={styles.rows}
                filter
                filterPlaceholder={'Search Name'}
                filterHeaderStyle={{ paddingTop: '0rem' }}
                sortable
              />
              <Column
                headerStyle={styles.tableHeader}
                style={styles.columns}
                bodyStyle={styles.rows}
                header='Last Modified Time'
                body={displayLastModifiedTime}
              />
              <Column
                headerStyle={styles.tableHeader}
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
