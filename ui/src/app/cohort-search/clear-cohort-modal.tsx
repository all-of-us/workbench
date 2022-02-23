import * as React from 'react';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

interface Props {
  onClear: Function;
  onClose: Function;
}

export const ClearCohortModal = (props: Props) => {
  const { onClear, onClose } = props;

  return (
    <Modal>
      <ModalTitle>Warning!</ModalTitle>
      <ModalBody>
        Any unsaved progress will be lost. Are you sure you want to clear this
        cohort?
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>
          Cancel
        </Button>
        <Button onClick={onClear}>Clear Cohort</Button>
      </ModalFooter>
    </Modal>
  );
};
