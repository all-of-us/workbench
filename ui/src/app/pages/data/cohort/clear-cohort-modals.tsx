import * as React from 'react';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

interface ClearProps {
  onClear: Function;
  onClose: Function;
}

interface DiscardProps {
  onClose: Function;
  onDiscard: Function;
}

export const CreateNewCohortModal = (props: ClearProps) => {
  const { onClear, onClose } = props;

  return (
    <Modal>
      <ModalTitle>Warning!</ModalTitle>
      <ModalBody>
        Any unsaved progress will be lost. Are you sure you want to start a new
        cohort?
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>
          Cancel
        </Button>
        <Button onClick={onClear}>New Cohort</Button>
      </ModalFooter>
    </Modal>
  );
};

export const DiscardCohortChangesModal = (props: DiscardProps) => {
  const { onClose, onDiscard } = props;

  return (
    <Modal>
      <ModalTitle>Warning!</ModalTitle>
      <ModalBody>
        Any unsaved progress will be lost. Are you sure you want to discard
        current changes to this cohort?
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>
          Cancel
        </Button>
        <Button onClick={onDiscard}>Discard Changes</Button>
      </ModalFooter>
    </Modal>
  );
};
