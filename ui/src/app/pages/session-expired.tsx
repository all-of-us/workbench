import {Component} from '@angular/core';
import {StyledAnchorTag} from 'app/components/buttons';
import {Header} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';

export class SessionExpired extends React.Component<{}, {}> {
  render() {
    return <PublicLayout>
      <Header >Session expired</Header>
      <p>You were automatically signed out of your session due to inactivity.</p>
      <p>Please <StyledAnchorTag href='/login'>sign in again</StyledAnchorTag>.</p>
    </PublicLayout>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class SessionExpiredComponent extends ReactWrapperBase {
  constructor() {
    super(SessionExpired, []);
  }
}
