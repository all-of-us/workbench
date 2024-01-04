import * as React from 'react';
import { useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AdminWorkspaceResources, ListRuntimeResponse } from 'generated/fetch';

import {
  fromRuntimeStatus,
  UIAppType,
  UserEnvironmentStatus,
} from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { getCreator } from 'app/utils/runtime-utils';

interface CloudEnvironmentRow {
  appType: UIAppType;
  name: string;
  creator: string;
  createdTime: string;
  lastAccessedTime: string;
  status: UserEnvironmentStatus;
}

const runtimeToRow = (runtime: ListRuntimeResponse): CloudEnvironmentRow => ({
  appType: UIAppType.JUPYTER,
  name: runtime.runtimeName,
  creator: getCreator(runtime),
  createdTime: new Date(runtime.createdDate).toDateString(),
  lastAccessedTime: new Date(runtime.dateAccessed).toDateString(),
  status: fromRuntimeStatus(runtime.status),
});

interface Props {
  resources: AdminWorkspaceResources;
  workspaceNamespace: string;
  onDelete: () => void;
}
export const CloudEnvironmentsTable = ({
  resources: { runtimes, userApps },
  workspaceNamespace,
  onDelete,
}: Props) => {
  const [runtimeToDelete, setRuntimeToDelete] = useState<string>();
  const [confirmDeleteRuntime, setConfirmDeleteRuntime] = useState(false);

  const deleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    workspaceAdminApi()
      .deleteRuntimesInWorkspace(workspaceNamespace, {
        runtimesToDelete: [runtimeToDelete],
      })
      .then(() => {
        setRuntimeToDelete(null);
        onDelete();
      });
  };

  const cancelDeleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    setRuntimeToDelete(null);
  };

  // const hasRuntimes = runtimes?.length > 0;
  // const hasUserApps = userApps?.length > 0;
  // const hasCloudEnvironments = hasRuntimes || hasUserApps;

  const cloudEnvironments: CloudEnvironmentRow[] = runtimes?.map(runtimeToRow);

  return (
    <div>
      {confirmDeleteRuntime && (
        <Modal onRequestClose={cancelDeleteRuntime}>
          <ModalTitle>Delete Runtime</ModalTitle>
          <ModalBody>
            This will immediately delete the given runtime. This will disrupt
            the user's work and may cause data loss.
            <br />
            <br />
            <b>Are you sure?</b>
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={cancelDeleteRuntime}>
              Cancel
            </Button>
            <Button style={{ marginLeft: '0.75rem' }} onClick={deleteRuntime}>
              Delete
            </Button>
          </ModalFooter>
        </Modal>
      )}
      <DataTable
        value={cloudEnvironments}
        emptyMessage='No active cloud environments exist for this workspace.'
      >
        <Column field='appType' header='Environment Type' />
        <Column field='name' header='Name' />
        <Column field='creator' header='Creator' />
        <Column field='createdTime' header='Created Time' />
        <Column field='lastAccessedTime' header='Last Accessed Time' />
        <Column field='status' header='Status' />
        <Column
          body={(row) => (
            <Button
              onClick={() => {
                setRuntimeToDelete(row.name);
                setConfirmDeleteRuntime(true);
              }}
              disabled={runtimeToDelete === row.name}
            >
              Delete
            </Button>
          )}
        />
      </DataTable>
    </div>
  );
};
