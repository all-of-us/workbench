import * as React from 'react';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';

interface Props {
  onClose: () => void;
}

export const VwbCreateWorkspaceModal = ({ onClose }: Props) => {
  return (
    <Modal
      data-test-id='vwb-create-workspace-modal'
      aria={{
        label: 'Create Workspace Redirect Modal',
      }}
    >
      {/* TITLE */}
      <ModalTitle>Researcher Workbench is moving</ModalTitle>

      {/* BODY */}
      <ModalBody>
        <div style={{ color: colors.dark, fontSize: '14px' }}>
          New workspaces can only be created in Researcher Workbench 2.0
        </div>
      </ModalBody>

      {/* FOOTER */}
      <ModalFooter style={{ justifyContent: 'flex-end', gap: '1rem' }}>
        <Button type='secondary' onClick={onClose}>
          Cancel
        </Button>

        <Button
          type='primary'
          onClick={() => {
            window.open(environment.vwbUiUrl, '_blank');
          }}
        >
          Open Verily Workbench
        </Button>
      </ModalFooter>
    </Modal>
  );
};
