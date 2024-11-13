import * as React from 'react';
import { useState } from 'react';

import { Button, StyledExternalLink } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { notificationStore } from 'app/utils/stores';
import { supportUrls } from 'app/utils/zendesk';

import { AoU } from './text-wrappers';

interface Props {
  onClose: Function;
}

const ExtensionDescription = () => (
  <div>
    Initial credits provided by the <AoU /> Research Program expire after X
    days. You are eligible to request an extension.
    <StyledExternalLink href={supportUrls.initialCredits}>
      Learn more about credits & setting up a billing account
    </StyledExternalLink>
  </div>
);

export const ExtendInitialCreditsModal = ({ onClose }: Props) => {
  const [saving, setSaving] = useState(false); // saving refers to the loading request time

  const onRequestExtension = () => {
    setSaving(true);
    profileApi()
      .extendInitialCreditExpiration()
      .then(() => {
        notificationStore.set({
          title: 'Initial credits extended',
          message: 'Initial credits will now expire on X',
        });
      })
      .catch((error: Response) => {
        error.json().then((errorJson) =>
          notificationStore.set({
            title: 'Error extending Initial credits',
            message: errorJson?.message,
          })
        );
      })
      .finally(() => onClose());
  };

  return (
    <Modal>
      <ModalTitle>Request Credit Expiration Date Extension</ModalTitle>
      <ModalBody style={{ marginTop: '0.3rem' }}>
        <ExtensionDescription />
      </ModalBody>
      <ModalFooter>
        <Button
          style={{ color: colors.primary }}
          type='link'
          onClick={() => onClose()}
          disabled={saving}
        >
          Cancel
        </Button>
        <Button type='primary' onClick={onRequestExtension} disabled={saving}>
          {saving && <Spinner style={{ marginRight: '0.375rem' }} size={18} />}
          Request Extension
        </Button>
      </ModalFooter>
    </Modal>
  );
};
