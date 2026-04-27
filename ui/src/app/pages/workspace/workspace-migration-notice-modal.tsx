import * as React from 'react';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';

interface Props {
  onClose: () => void;
}

export const WorkspaceMigrationNoticeModal = ({ onClose }: Props) => {
  return (
    <Modal
      data-test-id='workspace-migration-notice-modal'
      aria={{ label: 'Workspace Migration Notice' }}
    >
      <ModalTitle>Start planning for migration</ModalTitle>

      <ModalBody>
        <div style={{ color: colors.dark, fontSize: '14px' }}>
          The <AoU /> Researcher Workbench is moving to a new workbench platform
          soon. Prepare to migrate your workspaces.
        </div>
      </ModalBody>

      <ModalFooter style={{ justifyContent: 'flex-end' }}>
        <Button type='primary' onClick={onClose}>
          Got it!
        </Button>
      </ModalFooter>
    </Modal>
  );
};
