import * as React from 'react';

import { Button } from 'app/components/buttons';
import { Card } from 'app/components/card';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import { withErrorModal } from 'app/components/modals';
import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { signOut } from 'app/utils/authentication';
import { PRIVACY_WARNING_KEY } from 'app/utils/constants';
const styles = reactStyles({
  bodyText: {
    color: colors.primary,
    fontSize: '16px',
    marginBottom: '1rem',
  },
});
interface PrivacyWarningProps {
  onAcknowledge: () => void;
}
export const PrivacyWarning = ({ onAcknowledge }: PrivacyWarningProps) => {
  return (
    <FlexColumn style={{ maxWidth: '80vw', padding: '2rem 0rem 0rem 2rem' }}>
      <Card style={{ margin: '0rem 0rem 1rem 0rem' }}>
        <>
          <Header style={{ margin: '0rem 0rem 1rem 0rem' }}>
            Warning Notice
          </Header>
          <div style={styles.bodyText}>
            You are accessing a web site created by the <AoU /> Research
            Program, funded by the National Institutes of Health.
          </div>
          <div style={styles.bodyText}>
            Unauthorized attempts to upload information, change information, or
            use of this web site may result in disciplinary action, civil,
            and/or criminal penalties. Unauthorized users of this website should
            have no expectation of privacy regarding any communications or data
            processed by this website.
          </div>
          <div style={{ ...styles.bodyText, marginBottom: '0rem' }}>
            By continuing to log in, anyone accessing this website expressly
            consents to monitoring of their actions and all communications or
            data transiting or stored on related to this website and is advised
            that if such monitoring reveals possible evidence of criminal
            activity, NIH may provide that evidence to law enforcement
            officials.
          </div>
        </>
      </Card>
      <FlexRow style={{ marginTop: '0.5rem', gap: '0.5rem' }}>
        <Button
          aria-label='Acknowledge'
          style={{ margin: '0rem' }}
          onClick={() => {
            localStorage.setItem(
              PRIVACY_WARNING_KEY,
              new Date().toDateString()
            );
            onAcknowledge();
          }}
        >
          Acknowledge
        </Button>
        <Button
          type={'secondary'}
          onClick={withErrorModal(
            {
              title: 'Sign Out Error',
              message: 'There was an error signing out.',
            },
            () => signOut('/')
          )}
        >
          Cancel
        </Button>
      </FlexRow>
    </FlexColumn>
  );
};
