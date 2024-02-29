import * as React from 'react';
import { useState } from 'react';

import { Button } from 'app/components/buttons';
import { TextInput } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

export interface ConfirmWorkspaceDeleteModalProps {
  closeFunction: Function;
  receiveDelete: Function;
  workspaceName: string;
}
export const ConfirmWorkspaceDeleteModal = ({
  closeFunction,
  receiveDelete,
  workspaceName,
}: ConfirmWorkspaceDeleteModalProps) => {
  const [loading, setLoading] = useState(false);
  const [deleteDisabled, setDeleteDisabled] = useState(true);

  const emitDelete = () => {
    setLoading(true);
    receiveDelete();
  };

  const validateDeleteText = (event) => {
    setDeleteDisabled(!event.toLowerCase().match('delete'));
  };

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

          <TextInput
            placeholder='type DELETE to confirm'
            style={{ marginTop: '0.75rem' }}
            onChange={validateDeleteText}
            onBlur=''
          />
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
          disabled={loading || deleteDisabled}
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
