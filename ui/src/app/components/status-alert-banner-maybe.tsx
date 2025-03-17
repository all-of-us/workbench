import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlertLocation } from 'generated/fetch';

import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { firstPartyCookiesEnabled } from 'app/utils/cookies';
import {
  isDismissed,
  saveDismissedMessage,
} from 'app/utils/dismissed-messages';

import { Button } from './buttons';
import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastType } from './toast-banner';

const INITIAL_STATUS_ALERT: MultiToastMessage = {
  id: '',
  title: '',
  message: '',
  toastType: ToastType.WARNING,
};

const getDismissalId = (message: MultiToastMessage) =>
  `status-alert-${message.id}`;

const shouldShowStatusAlert = (message: MultiToastMessage) => {
  const messageId = getDismissalId(message);
  if (firstPartyCookiesEnabled()) {
    return !isDismissed(messageId);
  } else {
    return !!message;
  }
};

export const StatusAlertBannerMaybe = () => {
  const [showStatusAlert, setShowStatusAlert] = useState(false);
  const [alert, setAlert] = useState(INITIAL_STATUS_ALERT);

  useEffect(() => {
    const getAlert = async () => {
      const statusAlert = await statusAlertApi().getStatusAlert();

      if (
        statusAlert?.alertLocation === StatusAlertLocation.AFTER_LOGIN &&
        statusAlert?.title &&
        statusAlert?.message
      ) {
        const messageId = `status-alert-${statusAlert.statusAlertId}`;
        const message: MultiToastMessage = {
          id: messageId,
          title: statusAlert.title,
          message: statusAlert.message,
          toastType: ToastType.WARNING,
          footer: statusAlert.link ? (
            <Button onClick={() => window.open(statusAlert.link, '_blank')}>
              READ MORE
            </Button>
          ) : undefined,
        };
        setShowStatusAlert(shouldShowStatusAlert(message));
        setAlert(message);
      }
    };

    getAlert();
  }, []);

  const acknowledgeAlert = () => {
    saveDismissedMessage(getDismissalId(alert));
    setShowStatusAlert(false);
  };

  return showStatusAlert ? (
    <MultiToastBanner messages={[alert]} onDismiss={acknowledgeAlert} />
  ) : null;
};
