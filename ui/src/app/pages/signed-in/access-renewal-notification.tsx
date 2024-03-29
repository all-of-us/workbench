import * as React from 'react';

import { cond } from '@terra-ui-packages/core-utils';
import { NotificationBanner } from 'app/components/notification-banner';
import colors from 'app/styles/colors';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  ACCESS_RENEWAL_PATH,
  maybeDaysRemaining,
} from 'app/utils/access-utils';
import { profileStore, useStore } from 'app/utils/stores';

export interface AccessRenewalNotificationProps {
  accessTier: AccessTierShortNames;
}
export const AccessRenewalNotificationMaybe = (
  props: AccessRenewalNotificationProps
) => {
  const { profile } = useStore(profileStore);
  const daysRemaining = maybeDaysRemaining(profile, props.accessTier);

  const twoWeeksOrMoreRemaining = daysRemaining >= 14;
  const oneWeekOrMoreRemaining = daysRemaining >= 7;

  // special handling for Controlled Tier: don't render when RT is more urgent
  // because CT renewal is redundant in that case
  if (
    props.accessTier === AccessTierShortNames.Controlled &&
    maybeDaysRemaining(profile, AccessTierShortNames.Registered) <=
      daysRemaining
  ) {
    return null;
  }

  const accessType =
    props.accessTier === AccessTierShortNames.Controlled
      ? 'Controlled Tier access'
      : 'access';

  const timeLeft =
    daysRemaining >= 0
      ? daysRemaining + ' days remaining.'
      : 'Your access has expired.';

  const [boxColor, iconColor] = cond(
    [
      twoWeeksOrMoreRemaining,
      () => [
        colors.notificationBanner.expiring_after_two_weeks,
        colors.primary,
      ],
    ],
    [
      oneWeekOrMoreRemaining,
      () => [
        colors.notificationBanner.expiring_within_two_weeks,
        colors.warning,
      ],
    ],
    () => [colors.notificationBanner.expiring_within_one_week, colors.danger]
  );

  const boxStyle = { backgroundColor: boxColor };
  const iconStyle = { color: iconColor };
  // Must use pathname and search because ACCESS_RENEWAL_PATH includes a path with a search parameter.
  const { pathname, search } = window.location;
  const fullPagePath = pathname + search;

  // returning null is a way to tell React not to render this component.  `undefined` won't work here.
  return daysRemaining !== undefined ? (
    <NotificationBanner
      dataTestId='access-renewal-notification'
      text={`Time for ${accessType} renewal. ${timeLeft}`}
      buttonText='Get Started'
      buttonPath={ACCESS_RENEWAL_PATH}
      buttonDisabled={fullPagePath === ACCESS_RENEWAL_PATH}
      bannerTextWidth={
        props.accessTier === AccessTierShortNames.Registered ? '177px' : '250px'
      }
      {...{ boxStyle, iconStyle }}
    />
  ) : null;
};
