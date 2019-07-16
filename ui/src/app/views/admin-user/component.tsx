import {Component} from '@angular/core';
import * as React from 'react';

import {profileApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withUserProfile} from 'app/utils';
import {
  Profile,
} from 'generated/fetch';
import {SpinnerOverlay} from "../../components/spinners";
import {DataTable} from "primereact/datatable";
import {Column} from "primereact/column";

/**
 * Users with the ACCESS_MODULE_ADMIN permission use this
 * to manually set (approve/reject) the beta access state of a user, as well as
 * other access module bypasses.
 */
export const AdminUser = withUserProfile()(class extends React.Component<
  {profileState: {
    profile: Profile, reload: Function, updateCache: Function
  }}, {profiles: Profile[], contentLoaded: boolean}> {

  constructor(props) {
    super(props);
    this.state = {
      profiles: [],
      contentLoaded: false
    };
  }

  componentDidMount() {
    this.loadProfiles();
  }

  async loadProfiles() {
    this.setState({contentLoaded: false});
    profileApi().getAllUsers().then(profilesResp => {
      this.setState({profiles: this.sortProfileList(profilesResp.profileList),
        contentLoaded: true});
    });
  }

  // updateUserDisabledStatus(disable: boolean, profile: Profile): void {
  //   this.authDomainService.updateUserDisabledStatus(
  //     {email: profile.username, disabled: disable}).subscribe(() => {
  //     this.loadProfiles();
  //   });
  // }

  // We want to sort first by beta access status, then by
  // submission time (newest at the top), then alphanumerically.
  sortProfileList(profileList: Array<Profile>): Array<Profile> {
    return profileList.sort((a, b) => {
      // put disabled accounts at the bottom
      if (a.disabled && b.disabled) {
        return this.timeCompare(a, b);
      }
      if (a.disabled) {
        return 1;
      }
      if (!!a.betaAccessBypassTime === !!b.betaAccessBypassTime) {
        return this.timeCompare(a, b);
      }
      if (!!b.betaAccessBypassTime) {
        return -1;
      }
      return 1;
    });
  }

  private timeCompare(a: Profile, b: Profile): number {
    if (a.betaAccessRequestTime === b.betaAccessRequestTime) {
      return this.nameCompare(a, b);
    } else if (a.betaAccessRequestTime === null) {
      return 1;
    } else if (b.betaAccessRequestTime === null) {
      return -1;
    }
    return b.betaAccessRequestTime - a.betaAccessRequestTime;
  }

  private nameCompare(a: Profile, b: Profile): number {
    if (a.familyName === null) {
      return 1;
    }
    if (a.familyName.localeCompare(b.familyName) === 0) {
      if (a.givenName === null) {
        return 1;
      }
      return a.givenName.localeCompare(b.givenName);
    }
    return a.familyName.localeCompare(b.familyName);
  }

  render() {
    const {contentLoaded, profiles} = this.state;

    return <div style={{position: 'relative'}}>
      <h2>User Admin Table</h2>
      {contentLoaded ?
        <DataTable>
          <Column field='disabled' header='Disabled'/>
          <Column field='name' header='Name'/>
          <Column field='userName' header='User Name'/>
          <Column field='contactEmail' header='Contact Email'/>
          <Column field='betaAccessTimeRequested' header='Beta Access Time Requested'/>
          <Column field='userLockout' header='User Lockout'/>
          <Column field='bypass' header='Bypass'/>
        </DataTable> :
        <div>
          Loading user profiles...
          <SpinnerOverlay
            overrideStylesOverlay={{alignItems: 'flex-start', marginTop: '2rem'}}/>
        </div>}
    </div>;
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
