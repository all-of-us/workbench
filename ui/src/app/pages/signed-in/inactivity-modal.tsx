import * as React from 'react';

import { environment } from 'environments/environment';
import { TextModal } from 'app/components/text-modal';

export interface InactivityModalProps {
  currentTimeMs: number;
  signOutForInactivityTimeMs: number;
  inactivityWarningBeforeMs: number;
  closeFunction: () => void;
}

const secondsToText = (seconds: number) => {
  return seconds % 60 === 0 && seconds > 60
    ? `${seconds / 60} minutes`
    : `${seconds} seconds`;
};

export function InactivityModal({
  currentTimeMs,
  signOutForInactivityTimeMs,
  inactivityWarningBeforeMs,
  closeFunction,
}: InactivityModalProps) {
  const msUntilSignOut = signOutForInactivityTimeMs - currentTimeMs;

  if (msUntilSignOut > inactivityWarningBeforeMs) {
    return null;
  }

  const secondsBeforeDisplayingModal =
    environment.inactivityTimeoutSeconds -
    environment.inactivityWarningBeforeSeconds;

  return (
    <TextModal
      closeFunction={closeFunction}
      title='Your session is about to expire'
      body={
        `You have been idle for over ${secondsToText(
          secondsBeforeDisplayingModal
        )}. ` +
        'You can choose to extend your session by clicking the button below. You will be automatically logged ' +
        `out if there is no action in the next ${secondsToText(
          environment.inactivityWarningBeforeSeconds
        )}.`
      }
    />
  );
}
