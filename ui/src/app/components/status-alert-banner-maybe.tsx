import * as React from 'react';
import { useEffect, useState } from 'react';
import { StatusAlert, StatusAlertLocation } from 'generated/fetch';
import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import { Button } from './buttons';
import { ToastType } from './toast-banner';
import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { isDismissed, saveDismissedMessage } from 'app/utils/dismissed-messages';

const INITIAL_STATUS_ALERT: StatusAlert = {
  statusAlertId: 0,
  title: '',
  message: '',
  link: '',
};

const TEST_MESSAGE: MultiToastMessage = {
  id: 'test-alert',
  title: 'Test Alert',
  message: 'This is a test alert message to demonstrate multiple alerts.',
  toastType: ToastType.INFO,
  footer: (
    <Button onClick={() => window.open('https://researchallofus.org', '_blank')}>
      LEARN MORE
    </Button>
  )
};

export const StatusAlertBannerMaybe = () => {
  const [alertMessages, setAlertMessages] = useState<MultiToastMessage[]>([]);

  useEffect(() => {
    const getAlerts = async () => {
      const messages: MultiToastMessage[] = [];

      // Add API message if it exists and isn't dismissed
      const statusAlert = await statusAlertApi().getStatusAlert();
      if (statusAlert?.alertLocation === StatusAlertLocation.AFTER_LOGIN && 
          statusAlert.message) {
        const messageId = `status-alert-${statusAlert.statusAlertId}`;
        if (!isDismissed(messageId)) {
          const apiMessage: MultiToastMessage = {
            id: messageId,
            title: statusAlert.title,
            message: statusAlert.message,
            toastType: ToastType.WARNING,
            footer: statusAlert.link ? (
              <Button
                data-test-id='status-banner-read-more-button'
                onClick={() => window.open(statusAlert.link, '_blank')}
              >
                READ MORE
              </Button>
            ) : undefined
          };
          messages.push(apiMessage);
        }
      }

      // Add test message if it's not dismissed
      if (!isDismissed(TEST_MESSAGE.id)) {
        messages.push(TEST_MESSAGE);
      }

      setAlertMessages(messages);
    };

    getAlerts();
  }, []);

  const handleDismiss = (messageId: string) => {
    saveDismissedMessage(messageId);
    setAlertMessages(prev => prev.filter(msg => msg.id !== messageId));
  };

  if (alertMessages.length === 0) {
    return null;
  }

  return (
    <MultiToastBanner
      messages={alertMessages}
      baseZIndex={1000}
      onDismiss={handleDismiss}
    />
  );
};
