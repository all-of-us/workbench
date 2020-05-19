import {Component} from '@angular/core';
import {StyledAnchorTag} from 'app/components/buttons';
import {Header} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';

const supportUrl = 'support@researchallofus.org';

export class SignInAgain extends React.Component<{}, {}> {
  render() {
    return <PublicLayout>
      <Header>Sign in again</Header>
      <div style={{width: '500px'}}>
        <p>You’ve been away for a while and we cannot determine whether your session is still valid.</p>
        <p>Please <StyledAnchorTag href='/login'>sign in again</StyledAnchorTag> to keep your account secure.</p>
        <p>
          <strong>Note</strong>: You may have been redirected to this page immediately after attempting to sign in,
          if you did not explicitly sign out of your most recent session. If, after signing in
          again, you continue to be redirected to this page, please contact&nbsp;
          <StyledAnchorTag href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag> for
          assistance.
        </p>
      </div>
    </PublicLayout>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class SignInAgainComponent extends ReactWrapperBase {
  constructor() {
    super(SignInAgain, []);
  }
}
