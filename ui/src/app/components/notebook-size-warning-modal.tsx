import * as React from 'react';

import { Button, CloseButton, IconButton } from 'app/components/buttons';
import { WarningMessage } from 'app/components/messages';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';

import { FlexRow } from './flex';
import { SnowmanIcon } from './icons';

export const NotebookSizeWarningModal = () => {
  return (
    <Modal width={600}>
      <ModalTitle>
        <FlexRow>
          <div>Notebook file size bigger than 5mb</div>
          <CloseButton onClose={() => {}} style={{ marginLeft: 'auto' }} />
        </FlexRow>
      </ModalTitle>
      <ModalBody style={{ color: colors.primary }}>
        <WarningMessage>
          Opening this notebook may trigger your compute to be suspended because
          of its size. Please refer to this support article for instructions on
          how to clear the output of this notebook before you open it:
          <div style={{ padding: '0.5rem 0rem' }}>
            <a
              href='www.google.com'
              target='_blank'
              style={{ fontWeight: 'bold' }}
            >
              How to clear notebook outputs without editing them
            </a>
          </div>
          Either actions will incur cost and may trigger egress.
        </WarningMessage>
      </ModalBody>
      <ModalFooter>
        <Button type='secondary'>Run playground mode</Button>
        <Button type='primary'>Edit file</Button>
      </ModalFooter>
    </Modal>
  );
};
