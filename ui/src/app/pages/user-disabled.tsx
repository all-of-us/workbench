import {Component} from '@angular/core';
import * as React from 'react';

import {StyledAnchorTag} from 'app/components/buttons';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import colors from 'app/styles/colors';
import {
  ReactWrapperBase
} from 'app/utils';

const supportUrl = 'support@researchallofus.org';

export const UserDisabled: React.FunctionComponent<{}> = () => {
  return <PublicLayout>
    <BoldHeader>Your account has been disabled</BoldHeader>
    <section style={{color: colors.primary, fontSize: '18px', marginTop: '.5rem'}}>
      Contact <StyledAnchorTag href={'mailto:' + supportUrl}>{supportUrl}</StyledAnchorTag> for
      more information.
    </section>
  </PublicLayout>;
};

@Component({
  template: '<div #root></div>'
})
export class UserDisabledComponent extends ReactWrapperBase {
  constructor() {
    super(UserDisabled, []);
  }
}
