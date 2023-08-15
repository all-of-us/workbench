import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  getAccessModuleStatusByName,
  isCompliant,
} from 'app/utils/access-utils';
import { openZendeskWidget } from 'app/utils/zendesk';
import hhsLogo from 'assets/images/hhs-logo.png';
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
  onClick?: Function;
}) => {
  const { profile, afterInitialClick, onClick } = props;

  // don't return help text if complete or bypassed
  const needsHelp = !isCompliant(
    getAccessModuleStatusByName(profile, AccessModule.IDENTITY)
  );

  if (!needsHelp) {
    return null;
  }

  return (
    <FlexColumn style={styles.helpContainer}>
      {afterInitialClick ? (
        <div>
          Looks like you still need to complete this action, please try again.
        </div>
      ) : (
        <>
          <div>
            <a href='/' target='blank'>
              Click here
            </a>{' '}
            to review the verification steps.
          </div>
          <FlexColumn
            style={{
              padding: '1rem',
              backgroundColor: '#E8F1F8',
              borderRadius: '3px',
            }}
          >
            <FlexRow>
              <div style={styles.identityProviderDescription}>
                <img
                  src={loginGovLogo}
                  alt='Login Gov Logo'
                  style={{ height: '16px' }}
                />
                <div>
                  For <b>U.S. residents only</b>
                </div>
                <a
                  href='https://www.login.gov/help/verify-your-identity/how-to-verify-your-identity/'
                  target='_blank'
                >
                  View required documents
                </a>
              </div>
              <div style={styles.identityProviderDescription}>
                <img
                  src={idMeLogo}
                  alt='all of us logo'
                  style={{ height: '16px' }}
                />
                <div>
                  For <b>U.S. residents and international users</b>
                </div>
                <a
                  href='https://help.id.me/hc/en-us/articles/4415460350871-Documents-to-verify-your-identity'
                  target='_blank'
                >
                  View required documents
                </a>
              </div>
            </FlexRow>
          </FlexColumn>
          <FlexRow style={{ gap: '1rem' }}>
            <img src={hhsLogo} alt='HHS logo' style={{ height: '3rem' }} />
            <div>
              During the ID.ME verification process, you will be directed to a
              secure webpage hosted by the U.S.. Department of Health and Human
              Services external management system (<b>HHS XMS</b>). To proceed
              with data access, click on the <b>'Allow'</b> button to grant the
              necessary permissions.
            </div>
          </FlexRow>
          <Button style={{ alignSelf: 'end' }} onClick={onClick}>
            Get Started
          </Button>
        </>
      )}
      <ContactUs {...{ profile }} />
    </FlexColumn>
  );
};
