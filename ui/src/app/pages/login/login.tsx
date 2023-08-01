import * as React from 'react';

import { Button, StyledExternalLink } from 'app/components/buttons';
import { CookieBanner } from 'app/components/cookie-banner';
import { GoogleSignInButton } from 'app/components/google-sign-in';
import { Header, SmallHeader } from 'app/components/headers';
import { AouTitle } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  sign: {
    backgroundSize: 'contain',
    backgroundRepeat: 'noRepeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'spaceAround',
    alignItems: 'flexStart',
    width: 'auto',
  },
  createAccountButton: {
    fontSize: '12px',
    margin: '.75rem .75rem .375rem 0',
    border: `1px solid ${colors.primary}`,
    height: '48px',
    width: '9.75rem',
  },
  fismaCommon: {
    fontSize: '10px',
    color: colors.primary,
    lineHeight: '18px',
  },
  fismaHeader: {
    fontWeight: 600,
    textTransform: 'uppercase',
  },
  fismaSection: {
    fontWeight: 400,
    marginBottom: '0.75rem',
  },
});

interface LoginProps {
  onCreateAccount: Function;
}
export const LoginReactComponent = ({ onCreateAccount }: LoginProps) => {
  return (
    <React.Fragment>
      <div
        data-test-id='login'
        style={{
          marginBottom: '.75rem',
          marginTop: '8.25rem',
          paddingLeft: '4.5rem',
        }}
      >
        <div>
          <Header style={{ width: '21rem', lineHeight: '30px' }}>
            Already have a Researcher Workbench account?
          </Header>
          <div>
            <GoogleSignInButton />
          </div>
          <StyledExternalLink
            target='_blank'
            href='https://www.researchallofus.org/faq/what-if-i-have-trouble-signing-in-to-the-workbench'
            style={{ marginTop: '0.9375rem', lineHeight: '1.125rem' }}
          >
            Trouble Signing In?
          </StyledExternalLink>
        </div>
        <div style={{ paddingTop: '1.875rem' }}>
          <SmallHeader>Don't have an account?</SmallHeader>
          <Button
            aria-label='Create Account'
            type='secondary'
            style={styles.createAccountButton}
            onClick={onCreateAccount}
          >
            Create Account
          </Button>
        </div>
        <div style={{ width: '400px' }}>
          <h4 style={{ ...styles.fismaCommon, ...styles.fismaHeader }}>
            Warning Notice
          </h4>
          <div style={{ ...styles.fismaCommon, ...styles.fismaSection }}>
            You are accessing a web site created by the <AouTitle />, funded by
            the National Institutes of Health.
          </div>
          <div style={{ ...styles.fismaCommon, ...styles.fismaSection }}>
            Unauthorized attempts to upload information, change information, or
            use of this web site may result in disciplinary action, civil,
            and/or criminal penalties. Unauthorized users of this website should
            have no expectation of privacy regarding any communications or data
            processed by this website.
          </div>
          <div style={{ ...styles.fismaCommon, ...styles.fismaSection }}>
            By continuing to log in, anyone accessing this website expressly
            consents to monitoring of their actions and all communications or
            data transiting or stored on related to this website and is advised
            that if such monitoring reveals possible evidence of criminal
            activity, NIH may provide that evidence to law enforcement
            officials.
          </div>
        </div>
      </div>
      <CookieBanner />
    </React.Fragment>
  );
};

export default LoginReactComponent;
