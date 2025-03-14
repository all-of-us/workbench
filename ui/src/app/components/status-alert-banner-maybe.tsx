import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { firstPartyCookiesEnabled } from 'app/utils/cookies';

import { Button } from './buttons';
import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastBanner, ToastType } from './toast-banner';

const STATUS_ALERT_COOKIE_KEY = 'status-alert-banner-dismissed';
const INITIAL_STATUS_ALERT: MultiToastMessage = {
  id: '',
  title: '',
  message: '',
  toastType: ToastType.WARNING,
};

const shouldShowStatusAlert = (statusAlert: StatusAlert) => {
  const { statusAlertId, message } = statusAlert;
  if (firstPartyCookiesEnabled()) {
    const cookie = localStorage.getItem(STATUS_ALERT_COOKIE_KEY);
    return (!cookie || (cookie && cookie !== `${statusAlertId}`)) && !!message;
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

      if (statusAlert?.alertLocation === StatusAlertLocation.AFTER_LOGIN) {
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
        setShowStatusAlert(shouldShowStatusAlert(statusAlert));
        setAlert(message);
      }
    };

    getAlert();
  }, []);

  const acknowledgeAlert = () => {
    if (firstPartyCookiesEnabled()) {
      localStorage.setItem(STATUS_ALERT_COOKIE_KEY, `${alert.id}`);
    }
    setShowStatusAlert(false);
  };

  return showStatusAlert ? (
    <MultiToastBanner messages={[alert]} onDismiss={acknowledgeAlert} />
  ) : null;
};
