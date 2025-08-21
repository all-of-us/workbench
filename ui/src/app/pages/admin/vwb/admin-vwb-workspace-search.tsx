import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { VwbWorkspace, VwbWorkspaceSearchParamType } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Error, Select, TextInputWithLabel } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { useNavigation } from 'app/utils/navigation';

const VwbWorkspaceSearchParamTypeOptions = [
  {
    value: VwbWorkspaceSearchParamType.CREATOR,
    label: 'Workspace Creator',
  },
  {
    value: VwbWorkspaceSearchParamType.USER_FACING_ID,
    label: 'Workspace User Facing ID',
  },
  {
    value: VwbWorkspaceSearchParamType.NAME,
    label: 'Workspace Name',
  },
  {
    value: VwbWorkspaceSearchParamType.SHARED,
    label: 'Workspace Shared By/With',
  },
];

const SearchParamTypeLabel = {
  [VwbWorkspaceSearchParamType.CREATOR]: 'Creator Username',
  [VwbWorkspaceSearchParamType.USER_FACING_ID]: 'Workspace User Facing ID',
  [VwbWorkspaceSearchParamType.NAME]: 'Workspace Name',
  [VwbWorkspaceSearchParamType.SHARED]: 'Collaborator Username',
};

export const AdminVwbWorkspaceSearch = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [searchParam, setSearchParam] = useState('');
  const [searchParamType, setSearchParamType] =
    useState<VwbWorkspaceSearchParamType>(VwbWorkspaceSearchParamType.CREATOR);
  const [vwbWorkspaces, setVwbWorkspaces] = useState<VwbWorkspace[]>(null);
  const [fetchError, setFetchError] = useState(false);
  const [loading, setLoading] = useState(false);
  const [navigate] = useNavigation();

  useEffect(() => spinnerProps.hideSpinner(), []);

  const searchVwbWorkspaces = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      setVwbWorkspaces(null);
      const response =
        await vwbWorkspaceAdminApi().getVwbWorkspaceBySearchParam(
          searchParamType.toString(),
          searchParam
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
      <h2>Search VWB Workspaces</h2>
      <div style={headerStyles.formLabel}>Search by</div>
      <div style={{ width: '22.5rem', marginBottom: '1.5rem' }}>
        <Select
          value={searchParamType}
          options={VwbWorkspaceSearchParamTypeOptions}
          onChange={setSearchParamType}
        />
      </div>
      <TextInputWithLabel
        containerStyle={{ display: 'inline-block', marginRight: '1.5rem' }}
        style={{ width: '22.5rem', margin: '1.5rem' }}
        labelText={SearchParamTypeLabel[searchParamType]}
        value={searchParam}
        onChange={setSearchParam}
        onKeyDown={(event: KeyboardEvent) => {
          if (!!searchParam && event.key === 'Enter') {
            searchVwbWorkspaces();
          }
        }}
      />
      <Button
        style={{ height: '2.25rem' }}
        disabled={!searchParam}
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
              onRowClick={(row) =>
                navigate(['admin','vwb', 'workspaces', row.data.userFacingId])
              }
              emptyMessage='No workspaces found'
              loading={loading}
              value={vwbWorkspaces}
            >
              <Column
                field='userFacingId'
                header='User Facing ID'
                headerStyle={{ width: '250px' }}
              />
              <Column
                field='displayName'
                header='Name'
                headerStyle={{ width: '250px' }}
              />
              <Column field='description' header='Description' />
              <Column
                field='createdBy'
                header='Creator'
                headerStyle={{ width: '150px' }}
              />
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
