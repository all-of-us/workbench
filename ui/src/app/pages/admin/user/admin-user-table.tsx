import * as React from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AdminTableUser, Profile } from 'generated/fetch';

import { Button, StyledRouterLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { TierBadge } from 'app/components/tier-badge';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AdminUserBypass } from 'app/pages/admin/user/admin-user-bypass';
import {
  authDomainApi,
  userAdminApi,
} from 'app/services/swagger-fetch-clients';
import { reactStyles, usernameWithoutDomain, withUserProfile } from 'app/utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { getAdminUrl } from 'app/utils/institutions';
import { serverConfigStore } from 'app/utils/stores';
import moment from 'moment';

import { UserAuditLink } from './admin-user-common';

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
    minWidth: 1200,
  },
});

const LockoutButton: React.FunctionComponent<{
  disabled: boolean;
  profileDisabled: boolean;
  onClick: Function;
}> = ({ disabled, profileDisabled, onClick }) => {
  // We reduce the button height so it fits better within a table row.
  return (
    <Button
      type='secondaryLight'
      style={{ height: '40px' }}
      onClick={onClick}
      disabled={disabled}
    >
      {disabled ? (
        <Spinner size={25} />
      ) : profileDisabled ? (
        'Enable'
      ) : (
        'Disable'
      )}
    </Button>
  );
};

interface Props extends WithSpinnerOverlayProps {
  profileState: {
    profile: Profile;
    reload: Function;
    updateCache: Function;
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
export const AdminUserTable = withUserProfile()(
  class extends React.Component<Props, State> {
    debounceUpdateFilter: Function;
    constructor(props) {
      super(props);
      this.state = {
        contentLoaded: false,
        filter: '',
        loading: false,
        users: [],
      };
      this.debounceUpdateFilter = fp.debounce(300, (filterString) =>
        this.setState({ filter: filterString })
      );
    }

    async componentDidMount() {
      this.props.hideSpinner();
      this.setState({ contentLoaded: false });
      await this.loadProfiles();
      this.setState({ contentLoaded: true });
    }

    async loadProfiles() {
      const userListResponse = await userAdminApi().getAllUsers();
      this.setState({ users: userListResponse.users });
    }

    async updateUserDisabledStatus(disable: boolean, username: string) {
      this.setState({ loading: true });
      await authDomainApi().updateUserDisabledStatus({
        email: username,
        disabled: disable,
      });
      await this.loadProfiles();
      this.setState({ loading: false });
    }

    /**
     * Returns the appropriate cell contents (icon plus tooltip) for an access module cell
     * in the table.
     */
    accessModuleCellContents(
      user: AdminTableUser,
      key: string
    ): string | React.ReactElement {
      const completionTime = user[key + 'CompletionTime'];
      const bypassTime = user[key + 'BypassTime'];

      if (completionTime) {
        const completionTimeString = moment(completionTime).format('lll');
        return (
          <TooltipTrigger content={`Completed at ${completionTimeString}`}>
            <span>✔</span>
          </TooltipTrigger>
        );
      } else if (bypassTime) {
        const bypassTimeString = moment(bypassTime).format('lll');
        return (
          <TooltipTrigger content={`Bypassed at ${bypassTimeString}`}>
            <span>B</span>
          </TooltipTrigger>
        );
      } else {
        return '';
      }
    }

    dataAccessContents(user: AdminTableUser): JSX.Element {
      console.log(user.accessTierShortNames);
      return (
        <FlexRow style={{ justifyContent: 'space-evenly' }}>
          {user.accessTierShortNames.length > 0
            ? user.accessTierShortNames.map((accessTierShortName) => (
                <TierBadge {...{ accessTierShortName }} />
              ))
            : 'N/A'}
        </FlexRow>
      );
    }

    displayInstitutionName(tableRow: AdminTableUser) {
      const shouldShowLink =
        tableRow.institutionShortName &&
        hasAuthorityForAction(
          this.props.profileState.profile,
          AuthorityGuardedAction.INSTITUTION_ADMIN
        );
      return shouldShowLink ? (
        <StyledRouterLink
          path={getAdminUrl(tableRow.institutionShortName)}
          target='_blank'
        >
          {tableRow.institutionName}
        </StyledRouterLink>
      ) : (
        tableRow.institutionName
      );
    }

    /**
     * Returns a formatted timestamp, or an empty string if the timestamp is zero or null.
     */
    formattedTimestampOrEmptyString(timestampSecs: number | null): string {
      if (timestampSecs) {
        return moment.unix(timestampSecs / 1000).format('lll');
      } else {
        return '';
      }
    }

    convertProfilesToFields(users: AdminTableUser[]) {
      return users.map((user) => ({
        audit: (
          <UserAuditLink
            usernameWithoutDomain={usernameWithoutDomain(user.username)}
          >
            link
          </UserAuditLink>
        ),
        bypass: (
          <AdminUserBypass
            user={{ ...user }}
            onBypassModuleUpdate={() => this.loadProfiles()}
          />
        ),
        complianceTraining: this.accessModuleCellContents(
          user,
          'complianceTraining'
        ),
        ctComplianceTraining: this.accessModuleCellContents(
          user,
          'ctComplianceTraining'
        ),
        contactEmail: user.contactEmail,
        dataAccess: this.dataAccessContents(user),
        dataUseAgreement: this.accessModuleCellContents(
          user,
          'dataUseAgreement'
        ),
        eraCommons: this.accessModuleCellContents(user, 'eraCommons'),
        rasLinkLoginGov: this.accessModuleCellContents(user, 'rasLinkLoginGov'),
        firstSignInTime: this.formattedTimestampOrEmptyString(
          user.firstSignInTime
        ),
        firstSignInTimestamp: user.firstSignInTime,
        institutionName: this.displayInstitutionName(user),
        name: (
          <a
            href={`/admin/users/${usernameWithoutDomain(user.username)}`}
            target='_blank'
          >
            {user.familyName + ', ' + user.givenName}
          </a>
        ),
        nameText: user.familyName + ' ' + user.givenName,
        status: user.disabled ? 'Disabled' : 'Active',
        twoFactorAuth: this.accessModuleCellContents(user, 'twoFactorAuth'),
        username: user.username,
        userLockout: (
          <LockoutButton
            disabled={false}
            profileDisabled={user.disabled}
            onClick={() =>
              this.updateUserDisabledStatus(!user.disabled, user.username)
            }
          />
        ),
      }));
    }

    render() {
      const { contentLoaded, filter, loading, users } = this.state;
      const {
        enableComplianceTraining,
        enableEraCommons,
        enableRasLoginGovLinking,
      } = serverConfigStore.get().config;
      return (
        <div style={{ position: 'relative' }}>
          <h2>User Admin Table</h2>
          {loading && (
            <SpinnerOverlay
              opacity={0.6}
              overrideStylesOverlay={{
                alignItems: 'flex-start',
                marginTop: '2rem',
              }}
            />
          )}
          {!contentLoaded && <div>Loading user profiles...</div>}
          {contentLoaded && (
            <div>
              <input
                data-test-id='search'
                style={{ marginBottom: '.5em', width: '300px' }}
                type='text'
                placeholder='Search'
                onChange={(e) => this.debounceUpdateFilter(e.target.value)}
              />
              <DataTable
                value={this.convertProfilesToFields(users)}
                frozenWidth='200px'
                globalFilter={filter}
                paginator={true}
                rows={50}
                scrollable
                sortField={'firstSignInTimestamp'}
                style={styles.tableStyle}
              >
                <Column
                  field='name'
                  bodyStyle={{ ...styles.colStyle }}
                  filterField={'nameText'}
                  filterMatchMode={'contains'}
                  frozen={true}
                  header='Name'
                  headerStyle={{ ...styles.colStyle, width: '200px' }}
                  sortable={true}
                  sortField={'nameText'}
                />
                <Column
                  field='status'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Status'
                  headerStyle={{ ...styles.colStyle, width: '80px' }}
                />
                <Column
                  field='institutionName'
                  bodyStyle={{ ...styles.colStyle }}
                  header='Institution'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                />
                <Column
                  field='username'
                  bodyStyle={{ ...styles.colStyle }}
                  header='User Name'
                  headerStyle={{ ...styles.colStyle, width: '200px' }}
                  sortable={true}
                />
                <Column
                  field='contactEmail'
                  bodyStyle={{ ...styles.colStyle }}
                  header='Contact Email'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                />
                <Column
                  field='userLockout'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='User Lockout'
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                />
                <Column
                  field='firstSignInTime'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='First Sign-in'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                  sortField={'firstSignInTimestamp'}
                />
                <Column
                  field='dataAccess'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Data Access'
                  headerStyle={{ ...styles.colStyle, width: '100px' }}
                  sortable={false}
                />
                <Column
                  field='twoFactorAuth'
                  bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                  excludeGlobalFilter={true}
                  header='2FA'
                  headerStyle={{ ...styles.colStyle, width: '80px' }}
                />
                {enableComplianceTraining && (
                  <Column
                    field='complianceTraining'
                    bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                    excludeGlobalFilter={true}
                    header='RT RCR'
                    headerStyle={{ ...styles.colStyle, width: '80px' }}
                  />
                )}
                {enableComplianceTraining && (
                  <Column
                    field='ctComplianceTraining'
                    bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                    excludeGlobalFilter={true}
                    header='CT RCR'
                    headerStyle={{ ...styles.colStyle, width: '80px' }}
                  />
                )}
                {enableEraCommons && (
                  <Column
                    field='eraCommons'
                    bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                    excludeGlobalFilter={true}
                    header='eRA Commons'
                    headerStyle={{ ...styles.colStyle, width: '80px' }}
                  />
                )}
                <Column
                  field='dataUseAgreement'
                  bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                  excludeGlobalFilter={true}
                  header='DUCC'
                  headerStyle={{ ...styles.colStyle, width: '80px' }}
                />
                {enableRasLoginGovLinking && (
                  <Column
                    field='rasLinkLoginGov'
                    bodyStyle={{ ...styles.colStyle, textAlign: 'center' }}
                    excludeGlobalFilter={true}
                    header='RAS Login.gov Link'
                    headerStyle={{ ...styles.colStyle, width: '80px' }}
                  />
                )}
                <Column
                  field='bypass'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Bypass'
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                />
                <Column
                  field='audit'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Audit'
                  headerStyle={{ width: '60px' }}
                />
              </DataTable>
            </div>
          )}
        </div>
      );
    }
  }
);
