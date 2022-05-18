import * as React from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import { AdminTableUser, Profile } from 'generated/fetch';

import { AdminUserLink } from 'app/components/admin/admin-user-link';
import { StyledRouterLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { Ban, Check } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { TierBadge } from 'app/components/tier-badge';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AdminUserBypass } from 'app/pages/admin/user/admin-user-bypass';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import {
  cond,
  reactStyles,
  usernameWithoutDomain,
  withUserProfile,
} from 'app/utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { getAdminUrl } from 'app/utils/institutions';
import moment from 'moment';

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
  enabledIconStyle: {
    fontSize: 16,
    display: 'flex',
    margin: 'auto',
  },
});

const EnabledIcon = () => (
  <span>
    <Check style={styles.enabledIconStyle} />
  </span>
);
const DisabledIcon = () => (
  <span>
    <Ban style={styles.enabledIconStyle} />
  </span>
);

interface DataTableFields {
  name: JSX.Element;
  nameText: string;
  username: JSX.Element;
  usernameText: string;
  contactEmail: JSX.Element;
  contactEmailText: string;
  institutionName: JSX.Element | string;
  institutionNameText: string;
  enabled: JSX.Element;
  dataAccess: JSX.Element;
  bypass: JSX.Element;
  firstSignInTime: string;
}

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

    dataAccessContents(user: AdminTableUser): JSX.Element {
      return (
        <FlexRow style={{ justifyContent: 'space-evenly' }}>
          {cond(
            [user.disabled, () => <div>N/A</div>],
            [
              user.accessTierShortNames.length > 0,
              () => (
                <>
                  {user.accessTierShortNames.map((accessTierShortName) => (
                    <TierBadge
                      {...{ accessTierShortName }}
                      key={accessTierShortName}
                    />
                  ))}
                </>
              ),
            ],
            () => (
              <div>No Access</div>
            )
          )}
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

    convertProfilesToFields(users: AdminTableUser[]): DataTableFields[] {
      return users
        .map((user) => ({
          // 'text' fields are used for sorting

          name: (
            <AdminUserLink username={user.username} target='_blank'>
              {user.familyName + ', ' + user.givenName}
            </AdminUserLink>
          ),
          nameText: user.familyName + ' ' + user.givenName,

          username: (
            <AdminUserLink username={user.username} target='_blank'>
              {usernameWithoutDomain(user.username)}
            </AdminUserLink>
          ),
          usernameText: usernameWithoutDomain(user.username),

          contactEmail: (
            <a href={`mailto:${user.contactEmail}`}>{user.contactEmail}</a>
          ),
          contactEmailText: user.contactEmail,

          institutionName: this.displayInstitutionName(user),
          institutionNameText: user.institutionName,

          enabled: user.disabled ? <DisabledIcon /> : <EnabledIcon />,
          dataAccess: this.dataAccessContents(user),
          bypass: (
            <AdminUserBypass
              user={{ ...user }}
              onBypassModuleUpdate={() => this.loadProfiles()}
            />
          ),

          firstSignInTime: this.formattedTimestampOrEmptyString(
            user.firstSignInTime
          ),
        }))
        .sort((f1: DataTableFields, f2: DataTableFields) =>
          f1.nameText.localeCompare(f2.nameText)
        );
    }

    render() {
      const { contentLoaded, filter, loading, users } = this.state;
      return (
        <div style={{ position: 'relative', padding: '1em' }}>
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
                rows={10}
                rowsPerPageOptions={[5, 10, 50, 100]}
                scrollable
                style={styles.tableStyle}
              >
                <Column
                  field='name'
                  bodyStyle={{ ...styles.colStyle }}
                  filterField='nameText'
                  filterMatchMode='contains'
                  frozen={true}
                  header='Name'
                  headerStyle={{ ...styles.colStyle, width: '200px' }}
                  sortable={true}
                  sortField='nameText'
                />
                <Column
                  field='username'
                  bodyStyle={{ ...styles.colStyle }}
                  header='Username'
                  headerStyle={{ ...styles.colStyle, width: '200px' }}
                  sortable={true}
                  sortField='usernameText'
                />
                <Column
                  field='contactEmail'
                  bodyStyle={{ ...styles.colStyle }}
                  header='Contact Email'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                  sortField='contactEmailText'
                />
                <Column
                  field='institutionName'
                  bodyStyle={{ ...styles.colStyle }}
                  header='Institution'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                  sortField='institutionNameText'
                />
                <Column
                  field='enabled'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Enabled'
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                  sortable={false}
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
                  field='bypass'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='Access Module Bypass'
                  headerStyle={{ ...styles.colStyle, width: '150px' }}
                  sortable={false}
                />
                <Column
                  field='firstSignInTime'
                  bodyStyle={{ ...styles.colStyle }}
                  excludeGlobalFilter={true}
                  header='First Sign-in'
                  headerStyle={{ ...styles.colStyle, width: '180px' }}
                  sortable={true}
                  sortField='firstSignInTime'
                />
              </DataTable>
            </div>
          )}
        </div>
      );
    }
  }
);
