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

export const NotebookSizeWarningModal = (props: {
  handleClose?: () => void;
  handleEdit?: () => void;
  handlePlayground?: () => void;
}) => {
  const { handleClose, handleEdit, handlePlayground } = props;
  return (
    <Modal width={600}>
      <ModalTitle>
        <FlexRow>
          <div>Notebook file size bigger than 5mb</div>
          <CloseButton onClose={handleClose} style={{ marginLeft: 'auto' }} />
        </FlexRow>
      </ModalTitle>
      <ModalBody style={{ color: colors.primary }}>
        <WarningMessage>
          Opening this notebook may trigger your compute to be suspended because
          of its size. Please refer to this support article for instructions on
          how to clear the output of this notebook before you open it:
          <div style={{ padding: '0.5rem 0rem' }}>
            <a
              href='https://support.researchallofus.org/hc/en-us/articles/10916327500436-How-to-clear-notebook-outputs-without-editing-them'
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
        <Button type='secondary' onClick={handlePlayground}>
          Run playground mode
        </Button>
        <Button type='primary' onClick={handleEdit}>
          Edit file
        </Button>
      </ModalFooter>
    </Modal>
  );
};
