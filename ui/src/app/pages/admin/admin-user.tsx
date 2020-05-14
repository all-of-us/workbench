import {Component} from '@angular/core';
import * as React from 'react';
import {Profile} from 'generated/fetch';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {withUserProfile} from 'app/utils';
import {ReactWrapperBase} from 'app/utils';
import {FadeBox} from "app/components/containers";

const AdminUser = withUserProfile()(class extends React.Component<{
  profileState: {profile: Profile, reload: Function, updateCache: Function}
}> {
  constructor(props) {
    super(props)
  }

  render() {
    return <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '96.25%', minWidth: '1232px'}}>
      <FlexColumn>
        <FlexRow style={{alignItems: 'center'}}>
          <div>
            <ClrIcon shape='arrow' size={37} style={{
              backgroundColor: colorWithWhiteness(colors.accent, .85),
              color: colors.accent,
              borderRadius: '18px',
              transform: 'rotate(270deg)'
            }}/>
          </div>
          <SmallHeader>
            User Profile Information
          </SmallHeader>
        </FlexRow>
      </FlexColumn>
    </FadeBox>
  }
});

@Component({
  template: '<div #root></div>'
})
export class AdminUserComponent extends ReactWrapperBase {
  constructor() {
    super(AdminUser, []);
  }
}
