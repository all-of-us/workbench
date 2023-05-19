import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

interface Props {
  sourceUserEmail?: string;
  sourceWorkspaceNamespace?: string;
  displayPageSize?: number;
}

export const DisksTable = ({}: Props) => {
  const [loading] = useState(false);

  useEffect(() => {
    console.log('Initial load');
  }, []);

  return (
    <DataTable loading={loading}>
      <Column
        field='creationTime'
        header='Time'
        headerStyle={{ width: '150px' }}
        body={({ creationTime }) => new Date(creationTime).toLocaleString()}
      />
    </DataTable>
  );
};
