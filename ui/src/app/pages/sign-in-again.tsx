import * as React from 'react';
import {useEffect} from 'react';

import {GoogleSignInButton} from 'app/components/google-sign-in';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {SupportMailto} from 'app/components/support';

const styles = reactStyles({
  button: {
    fontSize: '16px',
    marginTop: '1rem'
  },
  textSection: {
    color: colors.primary,
    fontSize: '18px',
    marginTop: '.5rem'
  },
  noteSection: {
    color: colors.primary,
    fontSize: '14px',
    marginTop: '2rem'
  }
});

export const SignInAgain = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return <PublicLayout contentStyle={{width: '500px'}}>
    <BoldHeader>You have been signed out</BoldHeader>
    <section style={styles.textSection}>
      You’ve been away for a while and we could not verify whether your session was still active.
    </section>
    <GoogleSignInButton/>
    <section style={styles.noteSection}>
      <strong>Note</strong>: You may have been redirected to this page immediately after attempting to sign in,
      if you did not explicitly sign out of your most recent session. If, after signing in
      again, you continue to be redirected to this page, please contact <SupportMailto/> for assistance.
    </section>
  </PublicLayout>;
};
