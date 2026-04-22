import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { PreprodWorkspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Error, TextInputWithLabel } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { cdrVersionStore } from 'app/utils/stores';

export const AdminVwbPreprodMigration = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [preprodNamespace, setPreprodNamespace] = useState('');
  const [prodUsername, setProdUsername] = useState('');
  const [preprodWorkspaces, setPreprodWorkspaces] =
    useState<PreprodWorkspace[]>(null);
  const [fetchError, setFetchError] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => spinnerProps.hideSpinner(), []);

  const findPreprodWorkspace = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      setPreprodWorkspaces(null);
      const response = await vwbWorkspaceAdminApi().getPreprodWorkspace(
        preprodNamespace
      );
      setPreprodWorkspaces(response);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  };

  const handlePreprodMigration = async () => {};

  return (
    <div style={{ margin: '1.5rem' }}>
      <h2>Migrate Preprod Workspaces to RW 2.0</h2>
      <TextInputWithLabel
        containerStyle={{ display: 'inline-block', marginRight: '1.5rem' }}
        style={{ width: '22.5rem', margin: '1.5rem' }}
        labelText='Workspace namespace'
        value={preprodNamespace}
        onChange={setPreprodNamespace}
        onKeyDown={(event: KeyboardEvent) => {
          if (!!preprodNamespace && event.key === 'Enter') {
            findPreprodWorkspace();
          }
        }}
      />
      <Button
        style={{ height: '2.25rem' }}
        disabled={!preprodNamespace}
        onClick={() => findPreprodWorkspace()}
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
          preprodWorkspaces !== null && (
            <DataTable
              paginator
              rows={10}
              emptyMessage='No workspaces found'
              loading={loading}
              value={preprodWorkspaces}
            >
              <Column field='workspaceId' header='ID' />
              <Column field='workspaceNamespace' header='Workspace Namespace' />
              <Column field='displayName' header='Name' />
              <Column
                field='cdrVersionId'
                header='CDR Version'
                body={({ cdrVersionId }) => (
                  <div>
                    {findCdrVersion(cdrVersionId, cdrVersionStore.get()).name}
                  </div>
                )}
              />
              <Column
                field='accessTierShortName'
                header='Access Tier'
                style={{ textTransform: 'capitalize' }}
              />
              <Column
                body={() => (
                  <Button onClick={() => handlePreprodMigration()}>
                    Start Migration
                  </Button>
                )}
              />
            </DataTable>
          )
        )}
      </div>
    </div>
  );
};
