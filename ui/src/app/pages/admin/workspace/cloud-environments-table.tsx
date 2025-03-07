import * as React from 'react';
import { useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AdminRuntimeFields, UserAppEnvironment } from 'generated/fetch';

import {
  fromRuntimeStatus,
  fromUserAppStatusWithFallback,
  toUIAppType,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { getCreator } from 'app/utils/runtime-utils';

interface CloudEnvironmentRow {
  appType: UIAppType;
  name: string;
  creator: string;
  createdTime: string;
  lastAccessedTime: string;
  status: string;
}

const runtimeToRow = (runtime: AdminRuntimeFields): CloudEnvironmentRow => {
  const { runtimeName, createdDate, dateAccessed, status, labels } = runtime;
  return {
    appType: UIAppType.JUPYTER,
    name: runtimeName,
    creator: getCreator(labels),
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
    status: fromUserAppStatusWithFallback(status),
  };
};

interface DeleteButtonProps {
  row: CloudEnvironmentRow;
  runtimeToDelete?: string;
  setRuntimeToDelete: (runtimeName: string) => void;
  setConfirmDeleteRuntime: (confirmDelete: boolean) => void;
}
const DeleteButton = ({
  row,
  runtimeToDelete,
  setRuntimeToDelete,
  setConfirmDeleteRuntime,
}: DeleteButtonProps) => {
  const tooltipContent =
    row.appType === UIAppType.JUPYTER
      ? 'The persistent disk will not be deleted.'
      : 'Deletion is currently only available for Jupyter.  See RW-8943.';

  return (
    <TooltipTrigger content={tooltipContent}>
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
};

interface Props {
  workspaceNamespace: string;
  onDelete: () => void;
  runtimes?: AdminRuntimeFields[];
  userApps?: UserAppEnvironment[];
}
export const CloudEnvironmentsTable = ({
  workspaceNamespace,
  onDelete,
  runtimes,
  userApps,
}: Props) => {
  const [runtimeToDelete, setRuntimeToDelete] = useState<string | null>();
  const [confirmDeleteRuntime, setConfirmDeleteRuntime] = useState(false);

  const deleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    workspaceAdminApi()
      .adminDeleteRuntime(workspaceNamespace, runtimeToDelete)
      .then(() => {
        setRuntimeToDelete(null);
        onDelete();
      });
  };

  const cancelDeleteRuntime = () => {
    setConfirmDeleteRuntime(false);
    setRuntimeToDelete(null);
  };

  return runtimes && userApps ? (
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
        value={[
          ...(runtimes?.map(runtimeToRow) || []),
          ...(userApps?.map(userAppToRow) || []),
        ]}
        emptyMessage='No active cloud environments exist for this workspace.'
      >
        <Column field='appType' header='Environment Type' />
        <Column field='name' header='Name' />
        <Column field='creator' header='Creator' />
        <Column field='createdTime' header='Created Time' />
        <Column field='lastAccessedTime' header='Last Accessed Time' />
        <Column field='status' header='Status' />
        <Column
          body={(row: CloudEnvironmentRow) => (
            <DeleteButton
              {...{
                row,
                runtimeToDelete,
                setRuntimeToDelete,
                setConfirmDeleteRuntime,
              }}
            />
          )}
        />
      </DataTable>
    </div>
  ) : (
    <Spinner />
  );
};
