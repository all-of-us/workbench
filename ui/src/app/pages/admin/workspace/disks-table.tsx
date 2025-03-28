import * as React from 'react';
import { useEffect, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { Disk, ListDisksResponse } from 'generated/fetch';

import { toUIAppType, UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { disksAdminApi } from 'app/services/swagger-fetch-clients';
import { fetchWithErrorModal } from 'app/utils/errors';
import moment from 'moment';

interface Props {
  sourceWorkspaceNamespace?: string;
}
export const DisksTable = ({ sourceWorkspaceNamespace }: Props) => {
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [disks, setDisks] = useState<ListDisksResponse>([]);

  const refreshDisks = () => {
    setLoading(true);
    disksAdminApi()
      .listDisksInWorkspace(sourceWorkspaceNamespace)
      .then(setDisks)
      .finally(() => setLoading(false));
  };

  useEffect(() => refreshDisks(), []);

  const onClickDelete = (disk) =>
    fetchWithErrorModal(() => {
      setDeleting(true);
      return disksAdminApi().adminDeleteDisk(
        sourceWorkspaceNamespace,
        disk.name
      );
    }).finally(() => {
      setDeleting(false);
      refreshDisks();
    });

  return loading ? (
    <Spinner title='disks loading spinner' />
  ) : (
    <DataTable value={disks} emptyMessage='No disks found'>
      <Column
        body={(disk: Disk) => (
          <Button disabled={deleting} onClick={() => onClickDelete(disk)}>
            Delete
          </Button>
        )}
      />
      <Column field='name' header='Name' />
      <Column field='creator' header='Creator' />
      <Column
        field='createdDate'
        header='Date Created'
        body={(disk: Disk) =>
          moment(disk.createdDate).format('YYYY-MM-DD HH:mm')
        }
      />
      <Column field='status' header='Status' />
      <Column
        header='Environment Type'
        body={(disk: Disk) =>
          disk.gceRuntime ? UIAppType.JUPYTER : toUIAppType[disk.appType]
        }
      />
      <Column field='size' header='Size (GB)' />
    </DataTable>
  );
};
