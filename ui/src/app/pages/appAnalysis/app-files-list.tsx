import * as React from 'react';
import { useEffect, useState } from 'react';

import { ResourceType, WorkspaceResource } from 'generated/fetch';

import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { ResourceList } from 'app/components/resource-list';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { getExistingNotebooks } from 'app/pages/analysis/util';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { convertToResource } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

import { AppSelector } from './app-selector';

const styles = reactStyles({
  fadeBox: {
    margin: 'auto',
    marginTop: '1.5rem',
    width: '95.7%',
  },
});

interface AppFilesListProps extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}
export const AppFilesList = withCurrentWorkspace()(
  (props: AppFilesListProps) => {
    const { workspace } = props;
    const [notebookResources, setNotebookResources] =
      useState<WorkspaceResource[]>();

    const loadNotebooks = async () =>
      getExistingNotebooks(workspace)
        .then((notebookList) =>
          notebookList.map((notebook) =>
            convertToResource(notebook, ResourceType.NOTEBOOK, workspace)
          )
        )
        .then(setNotebookResources);

    useEffect(() => {
      props.hideSpinner();
      if (workspace) {
        loadNotebooks();
      }
    }, []);

    return (
      <FadeBox style={styles.fadeBox}>
        <FlexColumn>
          <FlexRow style={{ paddingBottom: '0.75rem' }}>
            <ListPageHeader style={{ paddingRight: '2.25rem' }}>
              Your Analyses
            </ListPageHeader>
            <AppSelector {...{ workspace }} />
          </FlexRow>
          {workspace && notebookResources && (
            <ResourceList
              workspaces={[workspace]}
              workspaceResources={notebookResources}
              onUpdate={() => loadNotebooks()}
            />
          )}
        </FlexColumn>
      </FadeBox>
    );
  }
);
