import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {displayNameForTier} from 'app/utils/access-tiers';

interface Props {
  accessTierShortName: string;
  onCancel: Function;
}

export const TierUnavailableModal = (props: Props) => {
  const {accessTierShortName, onCancel} = props;
  return <Modal onRequestClose={() => onCancel()}>
        <ModalTitle data-test-id='tier-unavailable-modal'>
          <div>You have selected the {displayNameForTier(accessTierShortName)} but you don't have access</div>
        </ModalTitle>
        <ModalBody>Before creating your workspace, please complete the data access requirements to gain access.</ModalBody>
        <ModalFooter>
          <Button type='secondary' onClick={() => onCancel()}>Cancel</Button>
          <Button type='primary' path='/data-access-requirements'>Get Started</Button>
        </ModalFooter>
    </Modal>;
};
