import * as React from 'react';
import { useEffect, useState } from 'react';
import { DataTable } from 'primereact/datatable';

import { Spinner } from 'app/components/spinners';

interface Props {
  sourceUserEmail?: string;
  sourceWorkspaceNamespace?: string;
  displayPageSize?: number;
}

export const DisksTable = ({}: Props) => {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    console.log('Initial load');
    setLoading(false);
  }, []);

  return loading ? <Spinner data-testid='disks spinner' /> : <DataTable />;
};
