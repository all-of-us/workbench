import * as React from 'react';

import { NotificationBanner } from 'app/components/notification-banner';
import {
  ACCESS_RENEWAL_PATH,
  maybeDaysRemaining,
} from 'app/utils/access-utils';
import { profileStore, useStore } from 'app/utils/stores';

export const AccessRenewalNotificationMaybe = () => {
  const { profile } = useStore(profileStore);
  const daysRemaining = maybeDaysRemaining(profile);
  const notificationText =
    'Time for access renewal. ' +
    `${
      daysRemaining >= 0
        ? daysRemaining + ' days remaining.'
        : 'Your access has expired.'
    }`;

  // Must use pathname and search because ACCESS_RENEWAL_PATH includes a path with a search parameter.
  const { pathname, search } = window.location;
  const fullPagePath = pathname + search;

  // returning null is a way to tell React not to render this component.  `undefined` won't work here.
  return daysRemaining !== undefined ? (
    <NotificationBanner
      dataTestId='access-renewal-notification'
      text={notificationText}
      buttonText='Get Started'
      buttonPath={ACCESS_RENEWAL_PATH}
      buttonDisabled={fullPagePath === ACCESS_RENEWAL_PATH}
    />
  ) : null;
};
