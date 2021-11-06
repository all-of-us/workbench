import {NotificationBanner} from 'app/components/notification-banner';
import {getAccessModuleStatusByName} from 'app/utils/access-utils';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule} from 'generated/fetch';
import * as React from 'react';

export const LoginGovIAL2NotificationMaybe = () => {
  const {profile} = useStore(profileStore);
  const {config: {enableRasLoginGovLinking}} = useStore(serverConfigStore);
  const loginGovModule = getAccessModuleStatusByName(profile, AccessModule.RASLINKLOGINGOV);
  // Show the Login.gov IAL2 notification when
  // 1: enableRasLoginGovLinking enabled AND
  // 2: user is not bypassed AND hasn't completed.
  // 3: loginGovModule undefined means the same thing as 2.
  const shouldShowIal2Notification = enableRasLoginGovLinking &&
      (!loginGovModule || (!loginGovModule.bypassEpochMillis && !loginGovModule.completionEpochMillis));
  return shouldShowIal2Notification
      ? <NotificationBanner
          dataTestId='ial2-notification'
          text='Please verify your identity by 1/31/2022.'
          buttonText='Get Started'
          buttonPath='/data-access-requirements'/>
      : null;
};
