import * as React from 'react';
import { useState } from 'react';

import { Profile } from 'generated/fetch';

import { Button, StyledExternalLink } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { displayDateWithoutHours, minusDays } from 'app/utils/dates';
import { notificationStore, serverConfigStore } from 'app/utils/stores';
import { supportUrls } from 'app/utils/zendesk';

import { AoU } from './text-wrappers';

const ExtensionDescription = ({ expirationDate }) => {
  const {
    initialCreditsValidityPeriodDays,
    initialCreditsExpirationWarningDays,
  } = serverConfigStore.get().config;

  const eligibilityDate = displayDateWithoutHours(
    minusDays(expirationDate, initialCreditsExpirationWarningDays)
  );

  return (
    <div>
      Initial credits provided by the <AoU /> Research Program expire after{' '}
      {initialCreditsValidityPeriodDays} days. You are eligible to request an
      extension after {eligibilityDate} which is{' '}
      {initialCreditsExpirationWarningDays} days before your current expiration
      date {displayDateWithoutHours(expirationDate)}.
      <StyledExternalLink
        href={supportUrls.initialCredits}
        style={{ marginTop: '1rem' }}
      >
        Learn more about credits & setting up a billing account
      </StyledExternalLink>
    </div>
  );
};

interface ExtendInitialCreditsModalProps {
  onClose: Function;
  expirationDate: number;
}

export const ExtendInitialCreditsModal = ({
  onClose,
  expirationDate,
}: ExtendInitialCreditsModalProps) => {
  const [saving, setSaving] = useState(false); // saving refers to the loading request time

  const onRequestExtension = () => {
    let updatedProfile: Profile;
    setSaving(true);
    profileApi()
      .extendInitialCreditExpiration()
      .then((profileResponse) => {
        updatedProfile = profileResponse;
        notificationStore.set({
          title: 'Initial credits extended',
          message:
            'Initial credits will now expire on ' +
            displayDateWithoutHours(
              profileResponse.initialCreditsExpirationEpochMillis
            ),
        });
      })
      .catch((error: Response) => {
        if (typeof error.json === 'function') {
          error.json().then((errorJson) =>
            notificationStore.set({
              title: 'Error extending Initial credits',
              message: errorJson?.message,
            })
          );
        } else {
          notificationStore.set({
            title: 'Error extending Initial credits',
            message: error.toString(),
          });
        }
      })
      .finally(() => onClose(updatedProfile));
  };

  return (
    <Modal>
      <ModalTitle>Request Credit Expiration Date Extension</ModalTitle>
      <ModalBody style={{ marginTop: '0.3rem' }}>
        <ExtensionDescription expirationDate={expirationDate} />
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
