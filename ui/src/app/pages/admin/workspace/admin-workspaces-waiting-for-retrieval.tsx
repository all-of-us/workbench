import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { WorkspaceRecoveryStatus, WorkspaceResponse } from 'generated/fetch';

import { Button, StyledRouterLink } from 'app/components/buttons';
import { ListPageHeader } from 'app/components/headers';
import { Error as ErrorMessage } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';

export const AdminWorkspacesWaitingForRetrieval = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [workspaceResponses, setWorkspaceResponses] = useState<
    WorkspaceResponse[]
  >([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(false);

  useEffect(() => spinnerProps.hideSpinner(), []);

  const loadWorkspaces = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      const response = await workspacesApi().getWorkspaces();
      setWorkspaceResponses(response.items || []);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkspaces();
  }, []);

  const waitingWorkspaces = useMemo(
    () =>
      workspaceResponses
        .filter(
          ({ workspace }) =>
            workspace.recoveryState === WorkspaceRecoveryStatus.REQUESTED
        )
        .sort(
          (a, b) =>
            (b.workspace.lastModifiedTime || 0) -
            (a.workspace.lastModifiedTime || 0)
        ),
    [workspaceResponses]
  );

  return (
    <div style={{ margin: '1.5rem' }}>
      <ListPageHeader>Workspaces Waiting for Retrieval</ListPageHeader>
      <p>
        These archived workspaces have recovery requested by researchers and are
        waiting for admin retrieval action.
      </p>

      <Button style={{ height: '2.25rem' }} onClick={() => loadWorkspaces()}>
        Refresh
      </Button>

      <div style={{ marginTop: '1.5rem' }}>
        {fetchError && (
          <ErrorMessage>
            Error loading data. Please refresh the page or contact the
            development team.
          </ErrorMessage>
        )}

        {loading ? (
          <SpinnerOverlay />
        ) : (
          <DataTable
            paginator
            rows={10}
            emptyMessage='No workspaces waiting for retrieval'
            value={waitingWorkspaces}
          >
            <Column
              header='Namespace'
              body={({ workspace }: WorkspaceResponse) => (
                <StyledRouterLink
                  path={`/admin/workspaces/${workspace.namespace}`}
                >
                  {workspace.namespace}
                </StyledRouterLink>
              )}
            />
            <Column
              header='Workspace Name'
              body={({ workspace }: WorkspaceResponse) =>
                workspace.displayName || workspace.name
              }
            />
            <Column
              header='Creator'
              body={({ workspace }: WorkspaceResponse) =>
                workspace.creatorUser?.userName || workspace.creator || 'N/A'
              }
            />
            <Column
              header='Last Updated'
              body={({ workspace }: WorkspaceResponse) =>
                workspace.lastModifiedTime
                  ? new Date(workspace.lastModifiedTime).toLocaleString()
                  : 'N/A'
              }
            />
          </DataTable>
        )}
      </div>
    </div>
  );
};
