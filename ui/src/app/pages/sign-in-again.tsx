import {Component} from '@angular/core';
import {Button, StyledAnchorTag} from 'app/components/buttons';
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
  },
  noteSection: {
    color: colors.primary,
    fontSize: '14px',
    marginTop: '2rem'
  }
});
const supportUrl = 'support@researchallofus.org';

export const SignInAgain: React.FunctionComponent<{signIn: Function}> = ({signIn}) => {
  return <PublicLayout contentStyle={{width: '500px'}}>
    <BoldHeader>You have been signed out</BoldHeader>
    <section style={styles.textSection}>
      You’ve been away for a while and we could not verify whether your session was still active.
    </section>
    <Button type='primary' style={styles.button} onClick={signIn}>
      Sign in again
    </Button>
    <section style={styles.noteSection}>
      <strong>Note</strong>: You may have been redirected to this page immediately after attempting to sign in,
      if you did not explicitly sign out of your most recent session. If, after signing in
      again, you continue to be redirected to this page, please contact&nbsp;
      <StyledAnchorTag href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag> for
      assistance.
    </section>
  </PublicLayout>;
};

@Component({
  template: '<div #root></div>'
})
export class SignInAgainComponent extends ReactWrapperBase {
  constructor(private signInService: SignInService) {
    super(SignInAgain, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
