import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { VwbDataCollectionEntry } from 'generated/fetch';

import { styles as headerStyles } from 'app/components/headers';
import { Error } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { vwbDataCollectionAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';

interface DataCollection {
  workspaceDisplayName: string;
  workspaceUserFacingId: string;
  workspaceDescription: string;
  createdDate: string;
  gcpProjectId: string;
  podId: string;
  resources: VwbDataCollectionEntry[];
}

export const AdminVwbDataCollections = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [entries, setEntries] = useState<VwbDataCollectionEntry[]>(null);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(false);
  const [selectedCollection, setSelectedCollection] =
    useState<DataCollection>(null);
  const [collectionFilter, setCollectionFilter] = useState('');
  const [resourceFilter, setResourceFilter] = useState('');

  const [debouncedSetCollectionFilter] = useState(() =>
    fp.debounce(300, (value: string) => setCollectionFilter(value))
  );
  const [debouncedSetResourceFilter] = useState(() =>
    fp.debounce(300, (value: string) => setResourceFilter(value))
  );

  const loadDataCollections = useCallback(async () => {
    try {
      setFetchError(false);
      setLoading(true);
      const response =
        await vwbDataCollectionAdminApi().listVwbDataCollections();
      setEntries(response.items);
    } catch (error) {
      console.error(error);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => spinnerProps.hideSpinner(), []);

  useEffect(() => {
    loadDataCollections();
  }, [loadDataCollections]);

  const collections: DataCollection[] = useMemo(() => {
    if (!entries) {
      return null;
    }
    const grouped = new Map<string, DataCollection>();
    for (const entry of entries) {
      const key = entry.workspaceUserFacingId;
      if (!grouped.has(key)) {
        grouped.set(key, {
          workspaceDisplayName: entry.workspaceDisplayName,
          workspaceUserFacingId: entry.workspaceUserFacingId,
          workspaceDescription: entry.workspaceDescription,
          createdDate: entry.createdDate,
          gcpProjectId: entry.gcpProjectId,
          podId: entry.podId,
          resources: [],
        });
      }
      if (entry.resourceId) {
        grouped.get(key).resources.push(entry);
      }
    }
    return Array.from(grouped.values());
  }, [entries]);

  const handleCollectionClick = (collection: DataCollection) => {
    setSelectedCollection(collection);
    setResourceFilter('');
  };

  return (
    <div style={{ margin: '1.5rem' }}>
      <h2>VWB Data Collections</h2>
      {fetchError && (
        <Error>
          Error loading data collections. Please refresh the page or contact the
          development team.
        </Error>
      )}
      {loading ? (
        <SpinnerOverlay />
      ) : (
        collections !== null && (
          <div style={{ display: 'flex', gap: '1.5rem' }}>
            {/* Data collections list */}
            <div style={{ flex: '0 0 45%' }}>
              <div style={headerStyles.formLabel}>Data Collections</div>
              <input
                data-test-id='collection-search'
                style={{ marginBottom: '.5em', width: '300px' }}
                type='text'
                placeholder='Search'
                onChange={(e) => debouncedSetCollectionFilter(e.target.value)}
              />
              <DataTable
                paginator
                rows={50}
                rowsPerPageOptions={[20, 50, 100, 500]}
                emptyMessage='No data collections found'
                value={collections}
                globalFilter={collectionFilter}
                selectionMode='single'
                selection={selectedCollection}
                onSelectionChange={(e) => {
                  const collection = e.value as DataCollection;
                  if (collection) {
                    handleCollectionClick(collection);
                  }
                }}
                dataKey='workspaceUserFacingId'
              >
                <Column
                  field='workspaceDisplayName'
                  header='Name'
                  sortable
                  filterField='workspaceDisplayName'
                  filterMatchMode='contains'
                />
                <Column
                  field='workspaceUserFacingId'
                  header='UFID'
                  sortable
                  filterField='workspaceUserFacingId'
                  filterMatchMode='contains'
                  headerStyle={{ width: '200px' }}
                />
                <Column
                  field='resources'
                  header='Resources'
                  headerStyle={{ width: '100px' }}
                  body={(dc: DataCollection) => dc.resources.length}
                  sortable
                  sortField='resources.length'
                  excludeGlobalFilter
                />
              </DataTable>
            </div>

            {/* Detail panel */}
            <div style={{ flex: '1' }}>
              {selectedCollection ? (
                <>
                  <div style={headerStyles.formLabel}>
                    {selectedCollection.workspaceDisplayName}
                  </div>
                  <div
                    style={{
                      marginBottom: '1rem',
                      fontSize: '13px',
                      color: colors.primary,
                      lineHeight: '1.4',
                    }}
                  >
                    {selectedCollection.workspaceDescription && (
                      <div style={{ marginBottom: '0.25rem' }}>
                        {selectedCollection.workspaceDescription}
                      </div>
                    )}
                    <div>
                      <strong>UFID:</strong>{' '}
                      {selectedCollection.workspaceUserFacingId}
                    </div>
                    <div>
                      <strong>GCP Project:</strong>{' '}
                      {selectedCollection.gcpProjectId}
                    </div>
                    <div>
                      <strong>Pod ID:</strong> {selectedCollection.podId}
                    </div>
                    {selectedCollection.createdDate && (
                      <div>
                        <strong>Created:</strong>{' '}
                        {selectedCollection.createdDate}
                      </div>
                    )}
                  </div>
                  <div
                    style={{
                      ...headerStyles.formLabel,
                      marginBottom: '0.25rem',
                    }}
                  >
                    Resources ({selectedCollection.resources.length})
                  </div>
                  <input
                    data-test-id='resource-search'
                    style={{ marginBottom: '.5em', width: '300px' }}
                    type='text'
                    placeholder='Search resources'
                    onChange={(e) => debouncedSetResourceFilter(e.target.value)}
                  />
                  <DataTable
                    paginator
                    rows={10}
                    rowsPerPageOptions={[5, 10, 50, 100]}
                    emptyMessage='No resources found'
                    value={selectedCollection.resources}
                    globalFilter={resourceFilter}
                  >
                    <Column
                      field='resourceDisplayName'
                      header='Display Name'
                      sortable
                      filterField='resourceDisplayName'
                      filterMatchMode='contains'
                    />
                    <Column
                      field='resourceName'
                      header='Name'
                      sortable
                      filterField='resourceName'
                      filterMatchMode='contains'
                    />
                    <Column
                      field='version'
                      header='Version'
                      sortable
                      headerStyle={{ width: '120px' }}
                      excludeGlobalFilter
                    />
                    <Column
                      field='resourceType'
                      header='Type'
                      sortable
                      headerStyle={{ width: '150px' }}
                      excludeGlobalFilter
                    />
                  </DataTable>
                </>
              ) : (
                <div
                  style={{
                    padding: '3rem',
                    textAlign: 'center',
                    color: colors.secondary,
                  }}
                >
                  Select a data collection to view its resources
                </div>
              )}
            </div>
          </div>
        )
      )}
    </div>
  );
};
