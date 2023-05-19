import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { disksAdminApi } from 'app/services/swagger-fetch-clients';

interface Props {
  sourceWorkspaceNamespace?: string;
}

export const DisksTable = ({ sourceWorkspaceNamespace }: Props) => {
  const [loading, setLoading] = useState(true);
  const [disks, setDisks] = useState([]);

  useEffect(() => {
    disksAdminApi()
      .listDisksInWorkspace(sourceWorkspaceNamespace)
      .then((value) => setDisks(value));
    setLoading(false);
  }, []);

  useEffect(() => console.log('What are disks? ', disks), [disks]);

  return loading && disks ? (
    <Spinner data-testid='disks spinner' />
  ) : (
    <DataTable value={disks}>
      <Column field='name' header='Name' />
      <Column field='creator' header='Creator' />
      <Column field='status' header='Status' />
      <Column field='size' header='Size (GB)' />
      <Column body={() => <Button>Delete</Button>} />
    </DataTable>
  );
};
