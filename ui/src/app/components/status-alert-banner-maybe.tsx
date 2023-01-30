import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlert } from 'generated/fetch';

import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { firstPartyCookiesEnabled } from 'app/utils/cookies';

import { Button } from './buttons';
import { ToastBanner, ToastType } from './toast-banner';

const STATUS_ALERT_COOKIE_KEY = 'status-alert-banner-dismissed';
const INITIAL_STATUS_ALERT: StatusAlert = {
  statusAlertId: 0,
  title: '',
  message: '',
  link: '',
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
  const [statusAlertDetails, setStatusAlertDetails] =
    useState(INITIAL_STATUS_ALERT);

  useEffect(() => {
    const getAlert = async () => {
      const statusAlert = await statusAlertApi().getStatusAlert();
      if (!!statusAlert) {
        setShowStatusAlert(shouldShowStatusAlert(statusAlert));
        setStatusAlertDetails(statusAlert);
      }
    };

    getAlert();
  }, []);

  const acknowledgeAlert = () => {
    if (firstPartyCookiesEnabled()) {
      localStorage.setItem(
        STATUS_ALERT_COOKIE_KEY,
        `${statusAlertDetails.statusAlertId}`
      );
    }
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
