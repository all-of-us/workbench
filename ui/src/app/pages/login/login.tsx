import * as React from 'react';
import { useEffect, useState } from 'react';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { Button, StyledExternalLink } from 'app/components/buttons';
import { CookieBanner } from 'app/components/cookie-banner';
import { GoogleSignInButton } from 'app/components/google-sign-in';
import { Header, SmallHeader } from 'app/components/headers';
import { statusAlertApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { LOGIN_ERROR_BANNER } from './login-error-banner';

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
  const [statusAlertDetails, setStatusAlertDetails] =
    useState<StatusAlert | null>(null);

  useEffect(() => {
    const getAlert = async () => {
      const statusAlert = await statusAlertApi().getStatusAlert();
      if (statusAlert) {
        setStatusAlertDetails(statusAlert);
      }
    };

    getAlert();
  }, []);

  return (
    <React.Fragment>
      {statusAlertDetails?.alertLocation ===
        StatusAlertLocation.BEFORE_LOGIN && (
        <LOGIN_ERROR_BANNER details={statusAlertDetails.message} />
      )}
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
      </div>
      <CookieBanner />
    </React.Fragment>
  );
};

export default LoginReactComponent;
