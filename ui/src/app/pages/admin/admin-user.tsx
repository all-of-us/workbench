import {Component} from '@angular/core';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {authDomainApi, profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {
  Profile,
} from 'generated/fetch';
import {AdminUserBypass} from 'app/pages/admin/admin-user-bypass';

const styles = reactStyles({
  colStyle: {
    lineHeight: '0.5rem',
    fontSize: 12
  },
  tableStyle: {
    fontSize: 12,
    minWidth: 1200
  }
});

const LockoutButton: React.FunctionComponent<{disabled: boolean,
  profileDisabled: boolean, onClick: Function}> =
  ({disabled, profileDisabled, onClick}) => {
    return <Button type='secondaryLight' onClick={onClick} disabled={disabled}>
      {disabled ? <Spinner size={25}/> : (profileDisabled ? 'Enable' : 'Disable')}
    </Button>;
  };

/**
 * Users with the ACCESS_MODULE_ADMIN permission use this
 * to manually set (approve/reject) the beta access state of a user, as well as
 * other access module bypasses.
 */
export const AdminUser = withUserProfile()(class extends React.Component<
  {profileState: {
    profile: Profile, reload: Function, updateCache: Function
  }}, {profiles: Profile[], contentLoaded: boolean, reloadingProfile: Profile}> {

  constructor(props) {
    super(props);
    this.state = {
      profiles: [],
      contentLoaded: false,
      reloadingProfile: null
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

  // To avoid reloading the entire list of profiles when we make a change to one user,
  //  reload the single user and update the list of profiles
  async reloadProfile(profile: Profile) {
    const profiles = this.state.profiles;
    this.setState({reloadingProfile: profile});
    const index = profiles.findIndex(x => x.username === profile.username);
    profileApi().getUser(profile.userId).then(updatedProfile => {
      profiles[index] = updatedProfile;
      this.setState({profiles: profiles, reloadingProfile: null});
    });
  }

  async updateUserDisabledStatus(disable: boolean, profile: Profile) {
    this.setState({reloadingProfile: profile});
    authDomainApi().updateUserDisabledStatus(
      {email: profile.username, disabled: disable}).then(_ => {
        this.reloadProfile(profile);
      });
  }

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

  convertDate(date): string {
    return new Date(date).toString().split(' ').slice(1, 5).join(' ');
  }

  convertProfilesToFields(profiles: Profile[]) {
    return profiles.map(p => ({...p, name: p.familyName + ', ' + p.givenName,
      betaAccessRequestTime: this.convertDate(p.betaAccessRequestTime),
      bypass: <AdminUserBypass profile={p}/>, disabled: p.disabled.toString(),
      userLockout: <LockoutButton disabled={this.state.reloadingProfile === p}
        profileDisabled={p.disabled}
        onClick={() => this.updateUserDisabledStatus(!p.disabled, p)}/>}));
  }

  render() {
    const {contentLoaded, profiles} = this.state;

    return <div style={{position: 'relative'}}>
      <h2>User Admin Table</h2>
      {contentLoaded ?
        <DataTable value={this.convertProfilesToFields(profiles)} style={styles.tableStyle}>
          <Column field='disabled' header='Disabled' bodyStyle={{...styles.colStyle, width: '7%'}}
                  headerStyle={{width: '7%'}}/>
          <Column field='name' header='Name' bodyStyle={{...styles.colStyle, width: '15%'}}
                  headerStyle={{width: '15%'}} sortable={true}/>
          <Column field='username' header='User Name' bodyStyle={{...styles.colStyle, width: '20%'}}
                  headerStyle={{width: '20%'}} sortable={true}/>
          <Column field='contactEmail' header='Contact Email' sortable={true}
                  bodyStyle={{...styles.colStyle, width: '19%'}} headerStyle={{width: '19%'}}/>
          <Column field='betaAccessRequestTime' header='Beta Access Time Requested' sortable={true}
                  bodyStyle={{...styles.colStyle, width: '15%'}} headerStyle={{width: '15%'}}/>
          <Column field='userLockout' header='User Lockout'
                  bodyStyle={{...styles.colStyle, width: '10%'}} headerStyle={{width: '10%'}}/>
          <Column field='bypass' header='Bypass'
                  bodyStyle={{...styles.colStyle, width: '10%'}} headerStyle={{width: '10%'}}/>
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
