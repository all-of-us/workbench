import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { FileDetail } from 'generated/fetch';

import { AppBanner } from 'app/components/apps-panel/app-banner';
import { StyledRouterLink } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { withErrorModal } from 'app/components/modals';
import { NotebookSizeWarningModal } from 'app/components/notebook-size-warning-modal';
import { NotebookActionMenu } from 'app/pages/analysis/notebook-action-menu';
import { getAppInfoFromFileName, listNotebooks } from 'app/pages/analysis/util';
import { analysisTabPath } from 'app/routing/utils';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
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

const notebookSizeThreshold = 5 * 1024 * 1024; // 5 MB
const transferCheckInterval = 10e3; // 10 seconds

export const errors = {
  filesError: {
    title: 'Error Loading Files',
    message:
      'Please refresh to try again.  ' +
      'If this problem persists, it may be because billing has been deactivated for this project, ' +
      'or that the bucket has been deleted.',
  },
};

const WaitingForFiles = () => (
  <FlexColumn style={{ paddingTop: '0.75rem' }}>
    <div>
      <FontAwesomeIcon
        style={{ color: colors.warning }}
        icon={faExclamationTriangle}
        size='2x'
      />
    </div>
    <div>Copying 1 or more notebooks from another workspace.</div>
    <div>
      Notebook copy should happen quickly, but it can sometimes take{' '}
      <b>minutes to hours</b> to complete.
    </div>
    <div>
      If it takes longer than a few minutes, try duplicating the original
      workspace again.
    </div>
  </FlexColumn>
);

interface AppFilesListProps {
  workspace: WorkspaceData;
}
export const AppFilesList = withCurrentWorkspace()(
  (props: AppFilesListProps) => {
    const { workspace } = props;

    // are we waiting for a duplicated workspace's files to transfer?
    const [isTransferComplete, setIsTransferComplete] =
      useState<boolean>(undefined);
    const [transferTimeoutId, setTransferTimeoutId] =
      useState<NodeJS.Timeout>(undefined);

    const [filesList, setFilesList] = useState<FileDetail[]>();
    const [showNotebookSizeWarningModal, setShowNotebookSizeWarningModal] =
      useState<boolean>(false);
    const [activeNotebookName, setActiveNotebookName] = useState<string>(null);

    const checkTransferComplete = withErrorModal(errors.filesError, () =>
      workspacesApi()
        .notebookTransferComplete(workspace.namespace, workspace.terraName)
        .then((isComplete) => {
          setIsTransferComplete(isComplete);

          if (!isComplete) {
            // check again after a delay, if we haven't scheduled a check already
            if (!transferTimeoutId) {
              const timeoutId = setTimeout(
                checkTransferComplete,
                transferCheckInterval
              );
              setTransferTimeoutId(timeoutId);
            }
          }
        })
    );

    const loadNotebooks = withErrorModal(errors.filesError, () =>
      listNotebooks(workspace).then(setFilesList)
    );

    useEffect(() => {
      if (workspace) {
        if (isTransferComplete) {
          loadNotebooks();
        } else {
          checkTransferComplete();
        }
      }
    }, [workspace, isTransferComplete]);

    const displayMenu = (row: FileDetail) => {
      return (
        <NotebookActionMenu
          {...{ workspace }}
          resource={convertToResources([row], props.workspace)[0]}
          useAppFilesListIcon={true}
          existingNameList={filesList.map((file) => file.name)}
          onUpdate={loadNotebooks}
        />
      );
    };

    const displayAppLogo = (row: FileDetail) => {
      // Find App Type on the basis of file name extension
      const { name } = row;
      const appType = getAppInfoFromFileName(name).appType;
      return <AppBanner appType={appType} style={{ marginRight: '1em' }} />;
    };

    const displayName = (row: FileDetail) => {
      const {
        workspace: { namespace, terraName },
      } = props;
      const { name } = row;
      const url = `${analysisTabPath(namespace, terraName)}/preview/${name}`;
      return row.sizeInBytes <= notebookSizeThreshold ? (
        <StyledRouterLink path={url} data-test-id='notebook-navigation'>
          {row.name}
        </StyledRouterLink>
      ) : (
        <div
          onClick={() => {
            setActiveNotebookName(row.name);
            setShowNotebookSizeWarningModal(true);
          }}
          style={{ color: '#6fb4ff', cursor: 'pointer' }}
        >
          {row.name}
        </div>
      );
    };

    const displayLastModifiedTime = (row: FileDetail) => {
      const time = displayDateWithoutHours(row.lastModifiedTime);
      return <div>{time}</div>;
    };

    // only show this screen when we know that the user needs to wait
    // but not on initial mount when it's undefined
    const showWaitingForFiles =
      !isTransferComplete && isTransferComplete !== undefined;

    return (
      <FadeBox style={styles.fadeBox}>
        {isTransferComplete && (
          <FlexColumn>
            <FlexRow style={styles.spacing}>
              <ListPageHeader
                aria-label='Your Analyses'
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
                rowsPerPageOptions={[10, 15, 25, 50]}
                data-test-id='apps-file-list'
                filterDisplay='row'
                value={filesList}
                paginator
                rows={10}
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
                  field='name'
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
                  field='lastModifiedTime'
                  sortable
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
        )}
        {showWaitingForFiles && <WaitingForFiles />}
        {showNotebookSizeWarningModal && (
          <NotebookSizeWarningModal
            namespace={workspace.namespace}
            terraName={workspace.terraName}
            notebookName={activeNotebookName}
            handleClose={() => {
              setShowNotebookSizeWarningModal(false);
              setActiveNotebookName(null);
            }}
          />
        )}
      </FadeBox>
    );
  }
);
