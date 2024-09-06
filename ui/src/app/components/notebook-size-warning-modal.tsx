import * as React from 'react';

import { Button, CloseButton } from 'app/components/buttons';
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

import { FlexRow } from './flex';
import { Spinner } from './spinners';

export interface NotebookSizeWarningModalProps {
  handleClose: () => void;
  namespace: string;
  terraName: string;
  notebookName: string;
}

const article =
  'https://support.researchallofus.org/hc/en-us/articles/10916327500436-How-to-clear-notebook-outputs-without-editing-them';
export const NotebookSizeWarningModal = ({
  handleClose,
  namespace,
  terraName,
  notebookName,
}: NotebookSizeWarningModalProps) => {
  const [navigate] = useNavigation();
  const navigationPath = [
    'workspaces',
    namespace,
    terraName,
    analysisTabName,
    notebookName,
  ];
  return (
    <Modal width={600}>
      <ModalTitle>
        <FlexRow>
          <div>Notebook file size bigger than 5MB</div>
          <CloseButton onClose={handleClose} style={{ marginLeft: 'auto' }} />
        </FlexRow>
      </ModalTitle>
      <ModalBody style={{ color: colors.primary, display: 'flex' }}>
        {notebookName ? (
          <WarningMessage iconPosition='top'>
            <div>
              Our system monitors network traffic levels to prevent downloading
              of unauthorized data. Loading or opening large notebooks may
              result in a spike to your network traffic that could trigger an
              egress alert.
            </div>
            <div style={{ marginTop: '1rem' }}>
              To prevent your account from being inadvertently suspended or
              disabled, it is recommended you clear your notebook output cells
              (see article below) to minimize notebook size and network traffic.
            </div>

            <div style={{ marginTop: '1rem' }}>
              <a href={article} target='_blank' style={{ fontWeight: 'bold' }}>
                How to clear notebook outputs without editing them
              </a>
            </div>

            <div style={{ marginTop: '1rem' }}>
              You may proceed editing or running this notebook in playground
              mode* but beware that you will incur cost and run the risk of your
              compute being suspended.
            </div>
            <div style={{ marginTop: '1rem' }}>
              *Playground mode allows users to run their notebooks but does not
              save any changes.
            </div>
          </WarningMessage>
        ) : (
          <Spinner style={{ margin: 'auto' }} />
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          disabled={!notebookName}
          type='secondary'
          onClick={() => {
            navigate(navigationPath, { queryParams: { playgroundMode: true } });
          }}
          style={{ marginRight: '1rem' }}
        >
          Run playground mode
        </Button>
        <Button
          disabled={!notebookName}
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
