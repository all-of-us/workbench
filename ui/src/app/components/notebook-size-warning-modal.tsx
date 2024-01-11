import * as React from 'react';

import { AppStatus, RuntimeStatus } from 'generated/fetch';

import { Button, CloseButton, IconButton } from 'app/components/buttons';
import { WarningMessage } from 'app/components/messages';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { analysisTabName } from 'app/routing/utils';
import colors from 'app/styles/colors';
import { useNavigation } from 'app/utils/navigation';

import { UIAppType } from './apps-panel/utils';
import { FlexRow } from './flex';
import { SnowmanIcon } from './icons';

export interface NotebookSizeWarningModalProps {
  handleClose: () => void;
  nameSpace: string;
  workspaceId: string;
  notebookName: string;
}
export const NotebookSizeWarningModal = ({
  handleClose,
  nameSpace,
  workspaceId,
  notebookName,
}: NotebookSizeWarningModalProps) => {
  const [navigate] = useNavigation();
  const navigationPath = [
    'workspaces',
    nameSpace,
    workspaceId,
    analysisTabName,
    notebookName,
  ];
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
        <Button
          type='secondary'
          onClick={() =>
            navigate(navigationPath, { queryParams: { playgroundMode: true } })
          }
        >
          Run playground mode
        </Button>
        <Button
          type='primary'
          onClick={() =>
            navigate(navigationPath, { queryParams: { playgroundMode: false } })
          }
        >
          Edit file
        </Button>
      </ModalFooter>
    </Modal>
  );
};
