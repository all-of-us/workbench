import * as React from 'react';

import { Button } from './buttons';
import { Modal, ModalBody, ModalFooter, ModalTitle } from './modals';

export const RuntimeErrorModal = ({
  errors,
  closeFunction,
  openRuntimePanel,
}) => {
  return (
    <Modal>
      <ModalTitle>Runtime Creation Error</ModalTitle>
      <ModalBody>
        <div>
          An error was encountered with your cloud environment. Please
          re-attempt creation of the environment and contact support if the
          error persists.
        </div>
        <div>Error details:</div>
        {errors.map((err, idx) => {
          return (
            <div style={{ fontFamily: 'monospace' }} key={idx}>
              {err.errorMessage}
            </div>
          );
        })}
      </ModalBody>
      <ModalFooter style={{ justifyContent: 'space-between' }}>
        <Button type='secondary' onClick={() => closeFunction()}>
          Close
        </Button>
        <Button
          onClick={() => {
            openRuntimePanel();
            closeFunction();
          }}
        >
          Customize Analysis Environment
        </Button>
      </ModalFooter>
    </Modal>
  );
};
