import * as React from 'react';
import { useEffect, useState } from 'react';

import { Button } from 'app/components/buttons';
import { TextInput } from 'app/components/inputs';
import { WarningMessage } from 'app/components/messages';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { appsApi } from 'app/services/swagger-fetch-clients';

export interface ConfirmWorkspaceDeleteModalProps {
  closeFunction: Function;
  receiveDelete: Function;
  workspaceName: string;
  workspaceNamespace: string;
}
export const ConfirmWorkspaceDeleteModal = ({
  closeFunction,
  receiveDelete,
  workspaceName,
  workspaceNamespace,
}: ConfirmWorkspaceDeleteModalProps) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [deleteDisabled, setDeleteDisabled] = useState<boolean>(true);
  const [checkingForUserApps, setCheckingForUserApps] = useState<boolean>(true);
  const [appsExistForWorkspace, setAppsExistForWorkspace] =
    useState<boolean>(false);

  useEffect(() => {
    setCheckingForUserApps(true);
    appsApi()
      .listAppsInWorkspace(workspaceNamespace)
      .then((userApps) => {
        setAppsExistForWorkspace(userApps?.length > 0);
        setCheckingForUserApps(false);
      })
      .finally(() => {
        setCheckingForUserApps(false);
      });
  }, []);

  const emitDelete = () => {
    setLoading(true);
    receiveDelete();
  };

  const validateDeleteText = (event) => {
    setDeleteDisabled(!event.toLowerCase().match('delete'));
  };

  const displayUserInput = !checkingForUserApps && !appsExistForWorkspace;

  return (
    <Modal loading={loading}>
      <ModalTitle style={{ lineHeight: '28px' }}>
        Warning — All work in this workspace will be lost.
      </ModalTitle>
      <ModalBody style={{ marginTop: '0.3rem', lineHeight: '28.px' }}>
        <div>
          <div>
            Are you sure you want to delete Workspace : {workspaceName}?
          </div>
          <br />
          <div>
            Deleting this workspace will immediately, permanently delete any
            items inside the workspace, such as notebooks and cohort
            definitions. This includes items created or used by other users with
            access to the workspace. If you still wish to delete this workspace
            and all items within it, type DELETE below to confirm.
          </div>
          {displayUserInput && (
            <TextInput
              placeholder='type DELETE to confirm'
              style={{ marginTop: '0.75rem' }}
              onChange={validateDeleteText}
              onBlur=''
            />
          )}
          {checkingForUserApps && (
            <div style={{ paddingTop: '1rem' }}>
              {' '}
              Checking for any existing User Apps....
            </div>
          )}
          {appsExistForWorkspace && (
            <WarningMessage>
              You cannot delete the workspace as there are Apps you must delete
              first
            </WarningMessage>
          )}
        </div>
      </ModalBody>
      <ModalFooter style={{ paddingTop: '1.5rem' }}>
        <Button
          aria-label='Cancel'
          type='secondary'
          onClick={() => closeFunction()}
        >
          Cancel
        </Button>
        <Button
          aria-label='Confirm Delete'
          disabled={
            loading ||
            deleteDisabled ||
            checkingForUserApps ||
            appsExistForWorkspace
          }
          style={{ marginLeft: '0.75rem' }}
          data-test-id='confirm-delete'
          onClick={() => emitDelete()}
        >
          Delete Workspace
        </Button>
      </ModalFooter>
    </Modal>
  );
};
