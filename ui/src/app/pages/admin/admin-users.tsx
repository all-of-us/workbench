import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {AdminUserBypass} from 'app/pages/admin/admin-user-bypass';
import {authDomainApi, profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, withUserProfile} from 'app/utils';
import {usernameWithoutDomain} from 'app/utils/audit-utils';
import {serverConfigStore} from 'app/utils/stores';
import {
  AdminTableUser,
  Profile,
} from 'generated/fetch';
import * as moment from 'moment';

const styles = reactStyles({
  colStyle: {
    fontSize: 12,
    height: '60px',
    lineHeight: '0.5rem',
    overflow: 'hidden',
    padding: '.5em',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  tableStyle: {
    fontSize: 12,
    minWidth: 1200
  }
});

const LockoutButton: React.FunctionComponent<{disabled: boolean,
  profileDisabled: boolean, onClick: Function}> =
  ({disabled, profileDisabled, onClick}) => {
    // We reduce the button height so it fits better within a table row.
    return <Button type='secondaryLight' style={{height: '40px'}} onClick={onClick} disabled={disabled}>
      {disabled ? <Spinner size={25}/> : (profileDisabled ? 'Enable' : 'Disable')}
    </Button>;
  };

interface Props {
  profileState: {
    profile: Profile, reload: Function, updateCache: Function
  };
}

interface State {
  contentLoaded: boolean;
  filter: string;
  loading: boolean;
  users: AdminTableUser[];
}

/**
 * Users with the ACCESS_MODULE_ADMIN permission use this
 * to manually set (approve/reject) access module bypasses.
 */
export const AdminUsers = withUserProfile()(class extends React.Component<Props, State> {
  debounceUpdateFilter: Function;
  constructor(props) {
    super(props);
    this.state = {
      contentLoaded: false,
      filter: '',
      loading: false,
      users: [],
    };
    this.debounceUpdateFilter = fp.debounce(300, (filterString) => this.setState({filter: filterString}));
  }

  async componentDidMount() {
    this.setState({contentLoaded: false});
    await this.loadProfiles();
    this.setState({contentLoaded: true});
  }

  async loadProfiles() {
    const userListResponse = await profileApi().getAllUsers();
    this.setState({users: userListResponse.users});
  }

  async updateUserDisabledStatus(disable: boolean, username: string) {
    this.setState({loading: true});
    await authDomainApi().updateUserDisabledStatus({email: username, disabled: disable});
    await this.loadProfiles();
    this.setState({loading: false});
  }

  // We want to sort first by submission time (newest at the top), then alphanumerically.
  sortProfileList(profileList: Array<Profile>): Array<Profile> {
    return profileList.sort((a, b) => {
      // put disabled accounts at the bottom
      if (a.disabled && b.disabled) {
        return this.nameCompare(a, b);
      }
      if (a.disabled) {
        return 1;
      }
      return 1;
    });
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

  /**
   * Returns the appropriate cell contents (icon plus tooltip) for an access module cell
   * in the table.
   */
  accessModuleCellContents(user: AdminTableUser, key: string): (string|React.ReactElement) {
    const completionTime = user[key + 'CompletionTime'];
    const bypassTime = user[key + 'BypassTime'];

    if (completionTime) {
      const completionTimeString = moment(completionTime).format('lll');
      return <TooltipTrigger content={`Completed at ${completionTimeString}`}>
        <span>✔</span>
      </TooltipTrigger>;
    } else if (bypassTime) {
      const bypassTimeString = moment(bypassTime).format('lll');
      return <TooltipTrigger content={`Bypassed at ${bypassTimeString}`}>
        <span>B</span>
      </TooltipTrigger>;
    } else {
      return '';
    }
  }

  /**
   * Returns a formatted timestamp, or an empty string if the timestamp is zero or null.
   */
  formattedTimestampOrEmptyString(timestampSecs: (number|null)): string {
    if (timestampSecs) {
      return moment.unix(timestampSecs / 1000).format('lll');
    } else {
      return '';
    }
  }

  convertProfilesToFields(users: AdminTableUser[]) {
    return users.map(user => ({
      audit: <a
        href={`/admin/user-audit/${usernameWithoutDomain(user.username)}`}
        target='_blank'>
        link
      </a>,
      bypass: <AdminUserBypass
        user={{...user}}
        onBypassModuleUpdate={() => this.loadProfiles()}/>,
      complianceTraining: this.accessModuleCellContents(user, 'complianceTraining'),
      contactEmail: user.contactEmail,
      dataUseAgreement: this.accessModuleCellContents(user, 'dataUseAgreement'),
      eraCommons: this.accessModuleCellContents(user, 'eraCommons'),
      rasLinkLoginGov: this.accessModuleCellContents(user, 'rasLinkLoginGov'),
      firstRegistrationCompletionTime: this.formattedTimestampOrEmptyString(user.firstRegistrationCompletionTime),
      firstRegistrationCompletionTimestamp: user.firstRegistrationCompletionTime,
      firstSignInTime: this.formattedTimestampOrEmptyString(user.firstSignInTime),
      firstSignInTimestamp: user.firstSignInTime,
      institutionName: user.institutionName,
      name: <a
        href={`/admin/users/${usernameWithoutDomain(user.username)}`}
        target='_blank'>
        {user.familyName + ', ' + user.givenName}
      </a>,
      nameText: user.familyName + ' ' + user.givenName,
      status: user.disabled ? 'Disabled' : 'Active',
      twoFactorAuth: this.accessModuleCellContents(user, 'twoFactorAuth'),
      username: user.username,
      userLockout: <LockoutButton disabled={false}
        profileDisabled={user.disabled}
        onClick={() => this.updateUserDisabledStatus(!user.disabled, user.username)}/>,
    }));
  }

  render() {
    const {contentLoaded, filter, loading, users} = this.state;
    const {enableComplianceTraining, enableEraCommons, enableDataUseAgreement, enableRasLoginGovLinking} = serverConfigStore.get().config;
    return <div style={{position: 'relative'}}>
      <h2>User Admin Table</h2>
      {loading &&
        <SpinnerOverlay opacity={0.6}
                        overrideStylesOverlay={{alignItems: 'flex-start', marginTop: '2rem'}}/>
      }
      {!contentLoaded && <div>Loading user profiles...</div>}
      {contentLoaded && <div>
        <input data-test-id='search'
               style={{marginBottom: '.5em', width: '300px'}}
               type='text'
               placeholder='Search'
               onChange={e => this.debounceUpdateFilter(e.target.value)}
        />
        <DataTable value={this.convertProfilesToFields(users)}
                   frozenWidth='200px'
                   globalFilter={filter}
                   paginator={true}
                   rows={50}
                   scrollable
                   sortField={'firstRegistrationCompletionTimestamp'}
                   style={styles.tableStyle}>
          <Column field='name'
                  bodyStyle={{...styles.colStyle}}
                  filterField={'nameText'}
                  filterMatchMode={'contains'}
                  frozen={true}
                  header='Name'
                  headerStyle={{...styles.colStyle, width: '200px'}}
                  sortable={true}
                  sortField={'nameText'}
          />
          <Column field='status'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='Status'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />
          <Column field='institutionName'
                  bodyStyle={{...styles.colStyle}}
                  header='Institution'
                  headerStyle={{...styles.colStyle, width: '180px'}}
                  sortable={true}
          />
          <Column field='firstRegistrationCompletionTime'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='Registration date'
                  headerStyle={{...styles.colStyle, width: '180px'}}
                  sortable={true}
                  sortField={'firstRegistrationCompletionTimestamp'}
          />
          <Column field='username'
                  bodyStyle={{...styles.colStyle}}
                  header='User name'
                  headerStyle={{...styles.colStyle, width: '200px'}}
                  sortable={true}
          />
          <Column field='contactEmail'
                  bodyStyle={{...styles.colStyle}}
                  header='Contact Email'
                  headerStyle={{...styles.colStyle, width: '180px'}}
                  sortable={true}
          />
          <Column field='userLockout'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='User Lockout'
                  headerStyle={{...styles.colStyle, width: '150px'}}
          />
          <Column field='firstSignInTime'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='First Sign-in'
                  headerStyle={{...styles.colStyle, width: '180px'}}
                  sortable={true}
                  sortField={'firstSignInTimestamp'}
          />
          <Column field='twoFactorAuth'
                  bodyStyle={{...styles.colStyle, textAlign: 'center'}}
                  excludeGlobalFilter={true}
                  header='2FA'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />
          {enableComplianceTraining && <Column field='complianceTraining'
                  bodyStyle={{...styles.colStyle, textAlign: 'center'}}
                  excludeGlobalFilter={true}
                  header='Training'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />}
          {enableEraCommons && <Column field='eraCommons'
                  bodyStyle={{...styles.colStyle, textAlign: 'center'}}
                  excludeGlobalFilter={true}
                  header='eRA Commons'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />}
          {enableDataUseAgreement && <Column field='dataUseAgreement'
                  bodyStyle={{...styles.colStyle, textAlign: 'center'}}
                  excludeGlobalFilter={true}
                  header='DUCC'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />}
          {enableRasLoginGovLinking && <Column field='rasLinkLoginGov'
                  bodyStyle={{...styles.colStyle, textAlign: 'center'}}
                  excludeGlobalFilter={true}
                  header='RAS Login.gov Link'
                  headerStyle={{...styles.colStyle, width: '80px'}}
          />}
          <Column field='bypass'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='Bypass'
                  headerStyle={{...styles.colStyle, width: '150px'}}
          />
          <Column field='audit'
                  bodyStyle={{...styles.colStyle}}
                  excludeGlobalFilter={true}
                  header='Audit'
                  headerStyle={{width: '60px'}}
          />
        </DataTable>
      </div>
      }
    </div>;
  }

});
