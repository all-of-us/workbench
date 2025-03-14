import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlertLocation } from 'generated/fetch';

import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import {
  isDismissed,
  saveDismissedMessage,
} from 'app/utils/dismissed-messages';

import { Button } from './buttons';
import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastType } from './toast-banner';

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
            if (
              alert.alertLocation === StatusAlertLocation.AFTER_LOGIN &&
              alert.message
            ) {
              const messageId = `status-alert-${alert.statusAlertId}`;
              if (!isDismissed(messageId)) {
                const apiMessage: MultiToastMessage = {
                  id: messageId,
                  title: alert.title,
                  message: alert.message,
                  toastType: ToastType.WARNING,
                  footer: alert.link ? (
                    <Button onClick={() => window.open(alert.link, '_blank')}>
                      READ MORE
                    </Button>
                  ) : undefined,
                };
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
    <MultiToastBanner messages={alertMessages} onDismiss={handleDismiss} />
  );
};
