import * as React from 'react';
import { useEffect, useState } from 'react';
import { DataTable } from 'primereact/datatable';

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

  return loading ? <Spinner data-testid='disks spinner' /> : <DataTable />;
};
