import {Component} from '@angular/core';
import * as React from 'react';

import {StyledAnchorTag} from 'app/components/buttons';
import {Header} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {
  ReactWrapperBase
} from 'app/utils';

const supportUrl = 'support@researchallofus.org';

export const UserDisabled = class extends React.Component<{}, {}> {
  render() {
    return <PublicLayout>
      <Header>Your account has been disabled</Header>
      <p>
        Contact <StyledAnchorTag href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag> for
        more information.
      </p>
    </PublicLayout>;
  }
};

@Component({
  template: '<div #root></div>'
})
export class UserDisabledComponent extends ReactWrapperBase {
  constructor() {
    super(UserDisabled, []);
  }
}
