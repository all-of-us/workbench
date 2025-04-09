import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import {
  isDismissed,
  saveDismissedMessage,
} from 'app/utils/dismissed-messages';
import { inRange } from 'app/utils/numbers';

import { Button } from './buttons';
import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastType } from './toast-banner';

const getMessageId = (statusAlert: StatusAlert) =>
  `status-alert-${statusAlert.statusAlertId}`;

const isAlertActive = (alert: StatusAlert): boolean =>
  alert.alertLocation === StatusAlertLocation.AFTER_LOGIN &&
  inRange(Date.now(), alert.startTimeEpochMillis, alert.endTimeEpochMillis);
const toToastMessage = (statusAlert: StatusAlert): MultiToastMessage => {
  return {
    id: getMessageId(statusAlert),
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

export const StatusAlertBannerMaybe = () => {
  const [alertMessages, setAlertMessages] = useState<MultiToastMessage[]>([]);

  useEffect(() => {
    const getAlerts = async () => {
      const messages: MultiToastMessage[] = [];

      try {
        const statusAlerts = await statusAlertApi().getStatusAlerts();
        if (statusAlerts && statusAlerts.length > 0) {
          // Process each alert and create a message if it's not dismissed
          statusAlerts.forEach((alert) => {
            if (isAlertActive(alert)) {
              if (!isDismissed(getMessageId(alert))) {
                const apiMessage: MultiToastMessage = toToastMessage(alert);
                messages.push(apiMessage);
              }
            }
          });
        }
      } catch (error) {
        console.error('Error fetching status alerts:', error);
      }

      setAlertMessages(messages);
    };

    getAlerts();
  }, []);

  const handleDismiss = (messageId: string) => {
    saveDismissedMessage(messageId);
    setAlertMessages((prev) => prev.filter((msg) => msg.id !== messageId));
  };

  if (alertMessages.length === 0) {
    return null;
  }

  return (
    <MultiToastBanner
      messages={alertMessages}
      onDismiss={handleDismiss}
      zIndex={1000}
    />
  );
};
