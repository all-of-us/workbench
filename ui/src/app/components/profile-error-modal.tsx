import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Button} from 'app/components/buttons';

import * as React from 'react';

export const ProfileErrorModal = ({title, message, onDismiss}) => {
  return <Modal data-test-id='profile-error-modal'>
    <ModalTitle>{title}</ModalTitle>
    <ModalBody>
        <div>An error occurred while saving profile. The following message was
            returned:
        </div>
        <div style={{marginTop: '1rem', marginBottom: '1rem'}}>
            "{message}"
        </div>
        <div>
            Please try again or contact <a
            href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
        </div>
    </ModalBody>
    <ModalFooter>
        <Button onClick={onDismiss} type='primary'>Close</Button>
    </ModalFooter>
  </Modal>
}
