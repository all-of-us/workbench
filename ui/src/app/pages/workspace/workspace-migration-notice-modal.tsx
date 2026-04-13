import * as React from 'react';

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

export const WorkspaceMigrationNoticeModal = ({ onClose }: Props) => {
  return (
    <Modal
      data-test-id='workspace-migration-notice-modal'
      aria={{ label: 'Workspace Migration Notice' }}
    >
      <ModalTitle>Start planning for migration</ModalTitle>

      <ModalBody>
        <div style={{ color: colors.dark, fontSize: '14px' }}>
          AoU Research will be transferring to a new service soon. Start putting
          together a plan for migration.
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
