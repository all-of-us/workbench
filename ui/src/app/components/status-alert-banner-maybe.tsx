import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

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

const getDismissalId = (statusAlert: StatusAlert) =>
  `status-alert-${statusAlert.statusAlertId}`;

const toToastMessage = (statusAlert: StatusAlert): MultiToastMessage => {
  return {
    id: getDismissalId(statusAlert),
    title: statusAlert.title,
    message: statusAlert.message,
    toastType: ToastType.WARNING,
    footer: statusAlert.link ? (
      <Button onClick={() => window.open(statusAlert.link, '_blank')}>
        READ MORE
      </Button>
    ) : undefined,
  };
};

const shouldShowStatusAlert = (statusAlert: StatusAlert) => {
  const messageId = getDismissalId(statusAlert);
  if (firstPartyCookiesEnabled()) {
    return !isDismissed(messageId);
  } else {
    return !!statusAlert;
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
        const message: MultiToastMessage = toToastMessage(statusAlert);
        setShowStatusAlert(shouldShowStatusAlert(statusAlert));
        setAlert(message);
      }
    };

    getAlert();
  }, []);

  const acknowledgeAlert = (dismissalId: string) => {
    saveDismissedMessage(dismissalId);
    setShowStatusAlert(false);
  };

  return showStatusAlert ? (
    <MultiToastBanner messages={[alert]} onDismiss={acknowledgeAlert} />
  ) : null;
};
