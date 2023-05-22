import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { disksAdminApi } from 'app/services/swagger-fetch-clients';
import moment from 'moment';

interface Props {
  sourceWorkspaceNamespace?: string;
}

export const DisksTable = ({ sourceWorkspaceNamespace }: Props) => {
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [disks, setDisks] = useState([]);

  const isDeletable = (diskStatus) => !['DELETED'].includes(diskStatus);

  useEffect(() => {
    if (loading) {
      disksAdminApi()
        .listDisksInWorkspace(sourceWorkspaceNamespace)
        .then((value) => setDisks(value))
        .finally(() => setLoading(false));
    }
  }, [loading]);

  useEffect(() => console.log('What are disks? ', disks), [disks]);

  return loading && disks ? (
    <Spinner data-testid='disks spinner' />
  ) : (
    <DataTable value={disks}>
      <Column field='name' header='Name' />
      <Column field='creator' header='Creator' />
      <Column
        field='createdDate'
        header='Date Created'
        body={(disk) => moment(disk.createdDate).format('YYYY-MM-DD HH:MM')}
      />
      <Column field='status' header='Status' />
      <Column field='size' header='Size (GB)' />
      <Column
        body={(disk) => (
          <Button
            disabled={isDeletable(disk.status) || deleting}
            onClick={() => {
              setDeleting(true);
            }}
          >
            Delete
          </Button>
        )}
      />
    </DataTable>
  );
};
