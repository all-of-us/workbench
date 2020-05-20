import {Component} from '@angular/core';
import {GoogleSignInButton} from 'app/components/google-sign-in';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {SignInService} from 'app/services/sign-in.service';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import * as React from 'react';

const styles = reactStyles({
  button: {
    fontSize: '16px',
    marginTop: '1rem'
  },
  textSection: {
    color: colors.primary,
    fontSize: '18px',
    marginTop: '.5rem'
  }
});

export const SessionExpired: React.FunctionComponent<{signIn: Function}> = ({signIn}) => {
  return <PublicLayout>
    <BoldHeader>You have been signed out</BoldHeader>
    <section style={styles.textSection}>
      You were automatically signed out of your session due to inactivity
    </section>
    <GoogleSignInButton signIn={signIn} />
  </PublicLayout>;
};

@Component({
  template: '<div #root></div>'
})
export class SessionExpiredComponent extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(SessionExpired, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
