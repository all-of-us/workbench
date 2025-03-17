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
import { ToastBanner, ToastType } from './toast-banner';

const INITIAL_STATUS_ALERT: StatusAlert = {
  statusAlertId: 0,
  title: '',
  message: '',
  link: '',
};

const shouldShowStatusAlert = (statusAlert: StatusAlert) => {
  const { statusAlertId, message } = statusAlert;
  const messageId = `status-alert-${statusAlertId}`;
  if (firstPartyCookiesEnabled()) {
    return !isDismissed(messageId);
  } else {
    return !!message;
  }
};

export const StatusAlertBannerMaybe = () => {
  const [showStatusAlert, setShowStatusAlert] = useState(false);
  const [statusAlertDetails, setStatusAlertDetails] =
    useState(INITIAL_STATUS_ALERT);

  useEffect(() => {
    const getAlert = async () => {
      const statusAlert = await statusAlertApi().getStatusAlert();
      if (
        statusAlert?.alertLocation === StatusAlertLocation.AFTER_LOGIN &&
        statusAlert?.title &&
        statusAlert?.message
      ) {
        setShowStatusAlert(shouldShowStatusAlert(statusAlert));
        setStatusAlertDetails(statusAlert);
      }
    };

    getAlert();
  }, []);

  const acknowledgeAlert = () => {
    saveDismissedMessage(`status-alert-${statusAlertDetails.statusAlertId}`);
    setShowStatusAlert(false);
  };

  const footer = statusAlertDetails.link && (
    <Button
      data-test-id='status-banner-read-more-button'
      onClick={() => window.open(statusAlertDetails.link, '_blank')}
    >
      READ MORE
    </Button>
  );

  return showStatusAlert ? (
    <ToastBanner
      title={statusAlertDetails.title}
      message={statusAlertDetails.message}
      onClose={() => acknowledgeAlert()}
      toastType={ToastType.WARNING}
      zIndex={1000}
      footer={footer}
    />
  ) : null;
};
