import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { VwbWorkspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Error, TextInputWithLabel } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';

export const AdminVwbWorkspace = (spinnerProps: WithSpinnerOverlayProps) => {
  const [workspaceCreator, setWorkspaceCreator] = useState('');
  const [vwbWorkspaces, setVwbWorkspaces] = useState<VwbWorkspace[]>(null);
  const [fetchError, setFetchError] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => spinnerProps.hideSpinner(), []);

  const searchVwbWorkspaces = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      setVwbWorkspaces(null);
      const response = await vwbWorkspaceAdminApi().getVwbWorkspaceByUsername(
        workspaceCreator
      );
      setVwbWorkspaces(response.items);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ margin: '1.5rem' }}>
      <h2>View VWB Workspaces By Creator</h2>
      <TextInputWithLabel
        containerStyle={{ display: 'inline-block', marginRight: '1.5rem' }}
        style={{ width: '22.5rem', margin: '1.5rem' }}
        labelText='Username'
        value={workspaceCreator}
        onChange={setWorkspaceCreator}
        onKeyDown={(event: KeyboardEvent) => {
          if (!!workspaceCreator && event.key === 'Enter') {
            searchVwbWorkspaces();
          }
        }}
      />
      <Button
        style={{ height: '2.25rem' }}
        disabled={!workspaceCreator}
        onClick={() => searchVwbWorkspaces()}
      >
        Get Workspaces
      </Button>
      <div style={{ marginTop: '1.5rem' }}>
        {fetchError && (
          <Error>
            Error loading data. Please refresh the page or contact the
            development team.
          </Error>
        )}
        {loading ? (
          <SpinnerOverlay />
        ) : (
          vwbWorkspaces !== null && (
            <DataTable
              paginator
              rows={10}
              emptyMessage='No workspaces found'
              loading={loading}
              value={vwbWorkspaces}
            >
              <Column
                field='userFacingId'
                header='Workspace ID'
                headerStyle={{ width: '250px' }}
              />
              <Column
                field='displayName'
                header='Name'
                headerStyle={{ width: '250px' }}
              />
              <Column field='description' header='Description' />
              <Column
                field='creationTime'
                header='Creation Time'
                headerStyle={{ width: '150px' }}
                body={({ creationTime }) =>
                  new Date(creationTime).toLocaleString()
                }
              />
            </DataTable>
          )
        )}
      </div>
    </div>
  );
};
