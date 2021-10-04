import * as React from 'react';

import {maybeDaysRemaining} from 'app/utils/access-utils';
import {profileStore, useStore} from 'app/utils/stores';
import {NotificationBanner} from 'app/components/notification-banner';

export const AccessRenewalNotificationMaybe = () => {
  const {profile} = useStore(profileStore);
  const daysRemaining = maybeDaysRemaining(profile);
  const notificationText = 'Time for access renewal. ' +
      `${daysRemaining >= 0 ? daysRemaining + ' days remaining.' : 'Your access has expired.'}`;
  // returning null is a way to tell React not to render this component.  `undefined` won't work here.
  return daysRemaining !== undefined
      ? <NotificationBanner
          dataTestId='access-renewal-notification'
          text={notificationText}
          buttonText='Get Started'
          buttonPath='/access-renewal'/>
      : null;
};
