import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import {
  getAccessModuleStatusByName,
  isCompliant,
} from 'app/utils/access-utils';
import { openZendeskWidget } from 'app/utils/zendesk';

import { styles } from './data-access-requirements';

const ContactUs = (props: { profile: Profile }) => {
  const {
    profile: { givenName, familyName, username, contactEmail },
  } = props;
  return (
    <div data-test-id='contact-us'>
      <span
        style={styles.link}
        onClick={(e) => {
          openZendeskWidget(givenName, familyName, username, contactEmail);
          // prevents the enclosing Clickable's onClick() from triggering instead
          e.stopPropagation();
        }}
      >
        Contact us
      </span>{' '}
      if you’re having trouble completing this step.
    </div>
  );
};

export const LoginGovHelpText = (props: {
  profile: Profile;
  afterInitialClick: boolean;
}) => {
  const { profile, afterInitialClick } = props;

  // don't return help text if complete or bypassed
  const needsHelp = !isCompliant(
    getAccessModuleStatusByName(profile, AccessModule.IDENTITY),
    profile.duccSignedVersion
  );

  return (
    needsHelp &&
    (afterInitialClick ? (
      <div style={styles.helpContainer}>
        <div>
          Looks like you still need to complete this action, please try again.
        </div>
        <ContactUs {...{ profile }} />
      </div>
    ) : (
      <div style={styles.helpContainer}>
        <div>
          Verifying your identity helps us keep participant data safe. You’ll
          need to provide your state ID, social security number, and phone
          number.
        </div>
        <ContactUs {...{ profile }} />
      </div>
    ))
  );
};
