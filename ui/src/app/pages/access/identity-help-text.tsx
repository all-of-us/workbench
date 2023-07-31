import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import {
  getAccessModuleStatusByName,
  isCompliant,
} from 'app/utils/access-utils';
import { openZendeskWidget } from 'app/utils/zendesk';
import idMeLogo from 'assets/images/id-me-logo.svg';
import loginGovLogo from 'assets/images/login-gov-logo.svg';

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

export const IdentityHelpText = (props: {
  profile: Profile;
  afterInitialClick: boolean;
}) => {
  const { profile, afterInitialClick } = props;

  // don't return help text if complete or bypassed
  const needsHelp = !isCompliant(
    getAccessModuleStatusByName(profile, AccessModule.RASLINKLOGINGOV)
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
        <div
          style={{
            padding: '1rem',
            backgroundColor: '#E8F1F8',
            borderRadius: '3px',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'row' }}>
            <div style={{ flex: 1 }}>
              <img
                src={loginGovLogo}
                alt='all of us logo'
                style={{ height: '16px' }}
              />
              <div>Roomba</div>
            </div>
            <div style={{ flex: 1 }}>
              <img
                src={idMeLogo}
                alt='all of us logo'
                style={{ height: '16px' }}
              />
              <div>iRobot</div>
            </div>
          </div>
        </div>
        <Button>Get Started</Button>
        <ContactUs {...{ profile }} />
      </div>
    ))
  );
};
