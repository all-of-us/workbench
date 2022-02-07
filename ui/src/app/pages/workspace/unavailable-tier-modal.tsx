import * as React from 'react';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { displayNameForTier } from 'app/utils/access-tiers';
import { DATA_ACCESS_REQUIREMENTS_PATH } from '../../utils/access-utils';

interface Props {
  accessTierShortName: string;
  onCancel: Function;
}

export const UnavailableTierModal = (props: Props) => {
  const { accessTierShortName, onCancel } = props;
  return (
    <Modal onRequestClose={() => onCancel()}>
      <ModalTitle data-test-id='unavailable-tier-modal'>
        <div>
          You have selected the {displayNameForTier(accessTierShortName)} but
          you don't have access
        </div>
      </ModalTitle>
      <ModalBody>
        Before creating your workspace, please complete the data access
        requirements to gain access.
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={() => onCancel()}>
          Cancel
        </Button>
        <Button type='primary' path={DATA_ACCESS_REQUIREMENTS_PATH}>
          Get Started
        </Button>
      </ModalFooter>
    </Modal>
  );
};
