import {Component} from '@angular/core';
import * as React from 'react';
import {Profile} from 'generated/fetch';

import {withUserProfile} from 'app/utils';
import {ReactWrapperBase} from 'app/utils';

const AdminUser = withUserProfile()(class extends React.Component<{
  profileState: {profile: Profile, reload: Function, updateCache: Function}
}> {
  constructor(props) {
    super(props)
  }

  render() {
    return <p>lol</p>
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
