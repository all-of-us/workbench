import * as React from 'react';
import { useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  AdminWorkspaceResources,
  ListRuntimeResponse,
  UserAppEnvironment,
} from 'generated/fetch';

import {
  fromRuntimeStatus,
  fromUserAppStatus,
  toUIAppType,
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
import { TooltipTrigger } from 'app/components/popups';
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

const runtimeToRow = (runtime: ListRuntimeResponse): CloudEnvironmentRow => {
  const { runtimeName, createdDate, dateAccessed, status } = runtime;
  return {
    appType: UIAppType.JUPYTER,
    name: runtimeName,
    creator: getCreator(runtime),
    createdTime: new Date(createdDate).toDateString(),
    lastAccessedTime: new Date(dateAccessed).toDateString(),
    status: fromRuntimeStatus(status),
  };
};

const userAppToRow = (userApp: UserAppEnvironment): CloudEnvironmentRow => {
  const { appType, appName, creator, createdDate, dateAccessed, status } =
    userApp;
  return {
    appType: toUIAppType[appType],
    name: appName,
    creator,
    createdTime: new Date(createdDate).toDateString(),
    lastAccessedTime: new Date(dateAccessed).toDateString(),
    status: fromUserAppStatus(status),
  };
};

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

  const DeleteButton = ({ row }: { row: CloudEnvironmentRow }) => (
    <TooltipTrigger
      content='Deletion is currently only availble for Jupyter.  See RW-8943.'
      disabled={row.appType === UIAppType.JUPYTER}
    >
      <Button
        disabled={
          row.appType !== UIAppType.JUPYTER || runtimeToDelete === row.name
        }
        onClick={() => {
          setRuntimeToDelete(row.name);
          setConfirmDeleteRuntime(true);
        }}
      >
        Delete
      </Button>
    </TooltipTrigger>
  );

  const cloudEnvironments = [
    ...runtimes?.map(runtimeToRow),
    ...userApps?.map(userAppToRow),
  ];

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
          body={(row: CloudEnvironmentRow) => <DeleteButton {...{ row }} />}
        />
      </DataTable>
    </div>
  );
};
