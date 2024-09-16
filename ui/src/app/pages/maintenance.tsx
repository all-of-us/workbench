import * as React from 'react';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexSpacer } from 'app/components/flex';
import { InfoMessage } from 'app/components/messages';
import { SUPPORT_EMAIL, SupportMailto } from 'app/components/support';
import logo from 'assets/images/all-of-us-logo.svg';

export const Maintenance = () => {
  return (
    <FlexColumn style={{ height: '100vh' }}>
      <FlexSpacer />
      <FlexColumn style={{ alignItems: 'center', flex: '0 0 1' }}>
        <img
          style={{ height: '15vh', marginBottom: '1rem' }}
          src={logo}
          alt='all of us logo'
        />
        <InfoMessage>
          <div style={{ maxWidth: '20vw' }}>
            <div>
              Our team is currently conducting maintenance on the application.
            </div>
            <div style={{ marginTop: '1rem' }}>
              Visit the{' '}
              <StyledExternalLink href='https://support.researchallofus.org/hc/en-us'>
                User Support Hub
              </StyledExternalLink>{' '}
              for additional information. If you have any questions, please
              contact us at <SupportMailto label={SUPPORT_EMAIL} />.
            </div>
          </div>
        </InfoMessage>
      </FlexColumn>
      <FlexSpacer />
    </FlexColumn>
  );
};
